package com.kuronami.gravely.event;

import java.util.ArrayList;
import java.util.List;

import com.kuronami.gravely.Config;
import com.kuronami.gravely.Gravely;
import com.kuronami.gravely.block.GraveBlock;
import com.kuronami.gravely.block.GraveBlockEntity;
import com.kuronami.gravely.data.GraveData;
import com.kuronami.gravely.registry.GravelyBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

/**
 * プレイヤー死亡時に墓ブロックを生成し、ドロップ + XP を墓に保管する。
 *
 * Phase 2 実装:
 *   - LivingDropsEvent でアイテム回収
 *   - LivingExperienceDropEvent で XP 回収 (Config.STORE_XP)
 *   - 設置位置探索を Config 駆動 (vertical/horizontal/voidFallbackY)
 *   - 流体・固体ブロックなど不適切な場所を避ける
 */
@EventBusSubscriber(modid = Gravely.MODID)
public final class DeathHandler {

    /**
     * プレイヤー死亡で抽出した XP を一時的に保持 (LivingDropsEvent と LivingExperienceDropEvent の同期用)
     *
     * 注意: NeoForge 1.21 系では LivingExperienceDropEvent → LivingDropsEvent の発火順序は
     * 実装上ほぼ保証されているが API 仕様としては明示されていない。逆転すると XP=0 で保管される。
     * 将来 NeoForge 仕様変更で問題が出たら、Player#getExperienceReward から直接取得する方式に切替。
     */
    private static final java.util.Map<java.util.UUID, Integer> PENDING_XP = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 死亡時に Soulbound 系で抽出されたアイテムを保留する。
     *
     * 重要: 死亡中の player に直接 inventory.add してもリスポーン時の新 player object には引き継がれない。
     * リスポーン後の player に対して inventory.add する必要がある。
     */
    private static final java.util.Map<java.util.UUID, java.util.List<ItemStack>> PENDING_SOULBOUND = new java.util.concurrent.ConcurrentHashMap<>();

    private DeathHandler() {}

    /** ログアウト時に PENDING_XP / PENDING_SOULBOUND のリーク防止 */
    @SubscribeEvent
    public static void onPlayerLoggedOut(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        java.util.UUID id = event.getEntity().getUUID();
        PENDING_XP.remove(id);
        PENDING_SOULBOUND.remove(id);
    }

    /** リスポーン時に Soulbound 保留アイテムを新 player の inventory へ追加 */
    @SubscribeEvent
    public static void onRespawn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        java.util.List<ItemStack> stacks = PENDING_SOULBOUND.remove(player.getUUID());
        if (stacks == null || stacks.isEmpty()) return;
        for (ItemStack stack : stacks) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        Gravely.LOGGER.info("Soulbound: restored {} item(s) to {} on respawn",
                stacks.size(), player.getName().getString());
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGH)
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (!Config.ENABLE_GRAVES.get() || !Config.STORE_XP.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // XP を墓に取り込みたい → そのドロップをキャンセル
        PENDING_XP.put(player.getUUID(), event.getDroppedExperience());
        event.setDroppedExperience(0);
    }

    /** 他のお墓MODより先に走るために HIGHEST 優先度 */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!Config.ENABLE_GRAVES.get()) return;

        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        // keepInventory が ON の場合は何もしない (ドロップ自体が無いはず)
        if (serverLevel.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            PENDING_XP.remove(player.getUUID());
            return;
        }

        // 全ドロップを ItemStack のリストとして抽出
        List<ItemStack> rawDrops = new ArrayList<>();
        event.getDrops().forEach(item -> rawDrops.add(item.getItem().copy()));

        // Curios slot を回収 (Curios 不在なら空リスト返却)
        rawDrops.addAll(com.kuronami.gravely.compat.CuriosCompat.collectAndClearItems(player));

        // Gravely Soulbound: 抽出して PENDING_SOULBOUND に保留 (リスポーン時に復元)
        List<ItemStack> rescued = new ArrayList<>();
        List<ItemStack> stacks = new ArrayList<>(com.kuronami.gravely.compat.GravelySoulboundHandler
                .extractSoulbound(player, rawDrops, rescued));
        if (!rescued.isEmpty()) {
            PENDING_SOULBOUND.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).addAll(rescued);
        }

        Integer xp = PENDING_XP.remove(player.getUUID());
        int storedXp = (xp != null && Config.STORE_XP.get()) ? xp : 0;

        if (stacks.isEmpty() && storedXp <= 0) return;

        // 設置位置を探す
        BlockPos placePos = findGravePos(serverLevel, player.blockPosition());
        if (placePos == null) {
            Gravely.LOGGER.info("Could not find a safe grave position for {}, falling back to drops",
                    player.getName().getString());
            // XP は復元できないが、アイテムは drop に任せる
            return;
        }

        // 墓を設置 (★水中なら waterlogged=true で配置 → 水が消えず流れない)
        FluidState fluidAtPos = serverLevel.getFluidState(placePos);
        boolean inWater = fluidAtPos.is(Fluids.WATER) || fluidAtPos.is(Fluids.FLOWING_WATER);
        BlockState graveState = GravelyBlocks.GRAVE.get().defaultBlockState()
                .setValue(GraveBlock.WATERLOGGED, inWater);
        if (!serverLevel.setBlock(placePos, graveState, Block.UPDATE_ALL)) {
            Gravely.LOGGER.warn("setBlock failed at {}", placePos);
            return;
        }

        BlockEntity be = serverLevel.getBlockEntity(placePos);
        if (!(be instanceof GraveBlockEntity grave)) {
            Gravely.LOGGER.error("BlockEntity at {} is not GraveBlockEntity", placePos);
            return;
        }

        // 弔いの鐘 (控えめな低音で「死を悼む」演出)
        serverLevel.playSound(null, placePos,
                net.minecraft.sounds.SoundEvents.BELL_RESONATE,
                net.minecraft.sounds.SoundSource.BLOCKS,
                0.5f, 0.7f);

        GraveData data = new GraveData(
                player.getUUID(),
                player.getName().getString(),
                stacks,
                storedXp,
                System.currentTimeMillis()
        );
        grave.setGraveData(data);

        // ドロップをキャンセル (墓に入れたので通常落下は不要)
        event.getDrops().clear();
        event.setCanceled(true);

        // 通知 (座標部分はクリック可能で /tp コマンド提案 + 紫色強調)
        if (Config.NOTIFY_GRAVE_CREATED.get()) {
            String tpCmd = "/tp @s " + placePos.getX() + " " + placePos.getY() + " " + placePos.getZ();
            Component coord = Component.literal(
                    placePos.getX() + ", " + placePos.getY() + ", " + placePos.getZ()
            ).withStyle(s -> s
                    .withColor(net.minecraft.ChatFormatting.LIGHT_PURPLE)
                    .withUnderlined(true)
                    .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                            net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND, tpCmd))
                    .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                            net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                            Component.translatable("message.gravely.grave_created_hover")))
            );
            player.displayClientMessage(
                    Component.translatable("message.gravely.grave_created", coord),
                    false
            );
        }

        // JourneyMap waypoint 登録用 payload (クライアント側で JM 判定するので常に送信)
        // ※ JM はクライアント専用MODなのでサーバ側では isLoaded=false。事前チェック禁止
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player,
                new com.kuronami.gravely.network.GravePosPayload(
                        placePos, serverLevel.dimension(), player.getName().getString()
                )
        );

        Gravely.LOGGER.info("Grave for {} created at {} (xp={}, items={})",
                player.getName().getString(), placePos, storedXp, stacks.size());
    }

    // ─────────────────────────────────────────────────
    // 設置位置探索 (Config 駆動、流体回避)
    // ─────────────────────────────────────────────────

    /**
     * 安全な設置位置を探す。
     * 戦略:
     *  1. 死亡座標から上下方向に最大 verticalSearch ブロック探索
     *  2. 各 Y で「ここが置換可能 + 足元が固体」を探す
     *  3. 見つからなければ XZ 方向にも horizontalSearch だけずらして探索
     *  4. 全部失敗したら voidFallbackY 高度で再試行
     *  5. それでも無理なら null
     */
    static BlockPos findGravePos(ServerLevel level, BlockPos origin) {
        int worldMinY = level.getMinBuildHeight();
        int worldMaxY = level.getMaxBuildHeight() - 1;
        int vSearch = Config.VERTICAL_SEARCH.get();
        int hSearch = Config.HORIZONTAL_SEARCH.get();
        int voidFallbackY = Config.VOID_FALLBACK_Y.get();

        // 死亡座標が虚空なら voidFallbackY から探す
        BlockPos start = origin.getY() < worldMinY
                ? new BlockPos(origin.getX(), Math.min(voidFallbackY, worldMaxY), origin.getZ())
                : origin;

        // 上下探索 → 同 XZ
        BlockPos found = scanColumn(level, start, vSearch, worldMinY, worldMaxY);
        if (found != null) return found;

        // XZ 周辺探索
        for (int r = 1; r <= hSearch; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue; // 円周のみ
                    BlockPos col = new BlockPos(origin.getX() + dx, start.getY(), origin.getZ() + dz);
                    BlockPos hit = scanColumn(level, col, vSearch, worldMinY, worldMaxY);
                    if (hit != null) return hit;
                }
            }
        }

        // 最終 fallback: voidFallbackY 高度で同 XZ を探す
        BlockPos fallback = new BlockPos(origin.getX(), Math.min(voidFallbackY, worldMaxY), origin.getZ());
        return scanColumn(level, fallback, vSearch, worldMinY, worldMaxY);
    }

    private static BlockPos scanColumn(ServerLevel level, BlockPos center, int vSearch, int worldMinY, int worldMaxY) {
        for (int dy = 0; dy <= vSearch; dy++) {
            for (int sign : new int[]{1, -1}) {
                int y = center.getY() + dy * sign;
                if (y < worldMinY + 1 || y >= worldMaxY) continue;
                BlockPos candidate = new BlockPos(center.getX(), y, center.getZ());
                if (isSafePlacement(level, candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static boolean isSafePlacement(ServerLevel level, BlockPos pos) {
        BlockState here = level.getBlockState(pos);
        BlockState below = level.getBlockState(pos.below());
        BlockState above = level.getBlockState(pos.above());

        // ここが置換可能 (air, grass, water 等) であること
        if (!here.canBeReplaced()) return false;

        // 真上の到達可能性 (Nether bedrock天井 / 1ブロック穴に埋め込まないため)
        // 流体は OK (水中の墓は真上も水なのが普通)
        if (!above.canBeReplaced() && above.getFluidState().isEmpty()) return false;

        // 流体チェック: 溶岩は常に NG、水は config 次第 (デフォルト ON)
        if (!here.getFluidState().isEmpty()) {
            if (here.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) return false;
            if (!Config.ALLOW_PLACE_IN_FLUID.get()) return false;
        }

        // 足元判定:
        //  - 空気: 浮いてしまうので NG
        //  - 溶岩: 危険・燃える可能性で NG
        //  - 水: ALLOW_PLACE_IN_FLUID なら OK (水中の墓を許可)
        //  - 固体: OK
        if (below.isAir()) return false;
        if (!below.getFluidState().isEmpty()) {
            if (below.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) return false;
            // 足元が水のとき、ALLOW_PLACE_IN_FLUID が ON なら OK
            return Config.ALLOW_PLACE_IN_FLUID.get();
        }
        // 流体じゃないなら固体性チェック
        return below.isSolid();
    }
}
