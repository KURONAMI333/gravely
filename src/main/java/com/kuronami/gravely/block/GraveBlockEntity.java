package com.kuronami.gravely.block;

import java.util.UUID;

import com.kuronami.gravely.Config;
import com.kuronami.gravely.Gravely;
import com.kuronami.gravely.data.GraveData;
import com.kuronami.gravely.registry.GravelyBlockEntities;
import com.kuronami.gravely.registry.GravelyDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 墓 BlockEntity。
 * Phase 2: アクセス制限 (OWNER_ONLY / TIMED / NONE) + OPバイパス + XP 復元 に対応。
 */
public class GraveBlockEntity extends BlockEntity {

    private GraveData graveData = GraveData.EMPTY;

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        super(GravelyBlockEntities.GRAVE.get(), pos, state);
    }

    public GraveData getGraveData() {
        return graveData;
    }

    public void setGraveData(GraveData data) {
        this.graveData = data == null ? GraveData.EMPTY : data;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * アクセス可否を Config 駆動で判定。
     * - OWNER_ONLY: 所有者のみ可
     * - TIMED:      所有者は常時可、他者は protectionDurationSec 経過後 OK
     * - NONE:       誰でも可
     * - OP バイパス: opCanBypass=true かつ permission level >= 2 で常時可
     */
    public boolean canAccess(Player player) {
        UUID playerId = player.getUUID();
        Config.ProtectionMode mode = Config.PROTECTION_MODE.get();

        if (mode == Config.ProtectionMode.NONE) return true;

        if (Config.OP_CAN_BYPASS.get()
                && player instanceof ServerPlayer sp
                && sp.hasPermissions(2)) {
            return true;
        }

        if (graveData.isOwner(playerId)) return true;

        if (mode == Config.ProtectionMode.TIMED) {
            return graveData.ageSeconds() >= Config.PROTECTION_DURATION_SEC.get();
        }
        // OWNER_ONLY
        return false;
    }

    /** 右クリック時のメイン処理。所有者なら全アイテム + XP を返却し墓を撤去。 */
    public InteractionResult tryRecoverBy(Player player) {
        if (graveData.isEmpty()) {
            removeSelf();
            return InteractionResult.SUCCESS;
        }
        if (!canAccess(player)) {
            Component msg;
            if (Config.PROTECTION_MODE.get() == Config.ProtectionMode.TIMED
                    && !graveData.isOwner(player.getUUID())) {
                // 他者で TIMED 保護中: 残り時間を表示
                long remainingSec = Math.max(0,
                        Config.PROTECTION_DURATION_SEC.get() - graveData.ageSeconds());
                msg = Component.translatable("message.gravely.timed_protected",
                        formatDuration(remainingSec));
            } else {
                msg = Component.translatable("message.gravely.grave_no_access",
                        graveData.ownerName().isEmpty() ? "?" : graveData.ownerName());
            }
            player.displayClientMessage(msg, true);
            return InteractionResult.FAIL;
        }

        // アイテム返却: まず Curios slot に着けられるものは戻す → 残りを通常インベントリ → さらに余ればドロップ
        java.util.List<ItemStack> remaining = com.kuronami.gravely.compat.CuriosCompat
                .tryEquipBack(player, graveData.nonEmptyItems());
        for (ItemStack stack : remaining) {
            if (!player.getInventory().add(stack.copy())) {
                player.drop(stack.copy(), false);
            }
        }

        // XP 返却
        if (graveData.totalXp() > 0) {
            player.giveExperiencePoints(graveData.totalXp());
        }

        // 回収演出 (パーティクル + 音) — ブロック撤去より前に出すこと
        if (level instanceof ServerLevel sl) {
            playRecoveryEffect(sl, this.worldPosition);

            // JourneyMap waypoint 削除用 payload (クライアント側で JM 判定するので常に送信)
            // ※ JM はクライアント専用MODなのでサーバ側では isLoaded=false。事前チェック禁止
            if (player instanceof ServerPlayer sp) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                        sp,
                        new com.kuronami.gravely.network.GraveRecoveredPayload(
                                this.worldPosition, sl.dimension()
                        )
                );
            }
        }

        graveData = GraveData.EMPTY;
        setChanged();

        if (Config.REMOVE_BLOCK_ON_RECOVER.get()) {
            removeSelf();
        } else {
            // ブロックは残すが見た目は中身空
            if (this.level != null && !this.level.isClientSide) {
                this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        player.displayClientMessage(
                Component.translatable("message.gravely.recovered"),
                true
        );
        return InteractionResult.SUCCESS;
    }

    /** ブロック破壊時の中身ドロップ (admin/op等が破壊した場合) */
    public void dropAllAt(Level level, BlockPos pos) {
        if (graveData.isEmpty()) return;
        for (ItemStack stack : graveData.nonEmptyItems()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy());
        }
        // XP は 経験値オーブとしてドロップ
        if (graveData.totalXp() > 0 && level instanceof net.minecraft.server.level.ServerLevel sl) {
            net.minecraft.world.entity.ExperienceOrb.award(
                    sl,
                    net.minecraft.world.phys.Vec3.atCenterOf(pos),
                    graveData.totalXp()
            );
        }
        graveData = GraveData.EMPTY;
        setChanged();
    }

    private void removeSelf() {
        if (level != null && !level.isClientSide) {
            level.removeBlock(this.worldPosition, false);
        }
    }

    /** 「123秒」「5分」「1時間30分」の形式に整形 */
    private static String formatDuration(long seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m";
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return minutes > 0 ? hours + "h " + minutes + "m" : hours + "h";
    }

    /** 回収成功時の演出: 紫の魔法粒 + 紫の閃光 + 軽快な拾得音 (Gravely ブランド紫) */
    private static void playRecoveryEffect(ServerLevel level, BlockPos pos) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        // 紫の魔法粒 (WITCH) - 「成仏感」、ブランドカラー紫と統一
        level.sendParticles(
                ParticleTypes.WITCH,
                cx, cy + 0.4, cz,
                16,                      // count
                0.4, 0.5, 0.4,            // x/y/z spread
                0.0                      // speed
        );
        // 紫の魔法光 (INSTANT_EFFECT) - 上方向にスーッと
        level.sendParticles(
                ParticleTypes.INSTANT_EFFECT,
                cx, cy + 1.2, cz,
                12,
                0.3, 0.4, 0.3,
                0.05
        );

        // 軽快な拾得音
        level.playSound(
                null,
                pos,
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.7f,
                1.3f
        );
        // 控えめなレベルアップ音 (祝福感)
        level.playSound(
                null,
                pos,
                SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS,
                0.25f,
                1.4f
        );
    }

    // ─────────────────────────────────────────────────
    // NBT 永続化
    // ─────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        GraveData.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, graveData)
                .resultOrPartial(err -> Gravely.LOGGER.error("Failed to encode GraveData: {}", err))
                .ifPresent(t -> tag.put("GraveData", t));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("GraveData")) {
            GraveData.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("GraveData"))
                    .resultOrPartial(err -> Gravely.LOGGER.error("Failed to decode GraveData: {}", err))
                    .ifPresent(d -> this.graveData = d);
        }
    }

    /**
     * クライアント同期用 NBT。
     *
     * 重要: GraveData (全アイテム + XP + 所有者) はクライアントに不要なので送らない。
     * 理由:
     *  - 墓ブロックのレンダリングは静的モデル (block/grave.json) で完結、BE データ不要
     *  - 全アイテム NBT を周辺の全プレイヤーへ送ると帯域問題 + プライバシー漏洩
     *    (Sophisticated Backpacks 等の大量アイテム持ちの墓で顕著)
     *  - GraveData は所有者の右クリック時のみサーバ側で参照される
     *
     * 将来 GUI で「誰の墓か」を表示するなら、ownerName だけここで追加する。
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return super.getUpdateTag(registries);
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        GraveData data = input.get(GravelyDataComponents.GRAVE_DATA.get());
        if (data != null) {
            this.graveData = data;
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(GravelyDataComponents.GRAVE_DATA.get(), graveData);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("GraveData");
    }
}
