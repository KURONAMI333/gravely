package com.kuronami.gravely.compat;

import java.util.ArrayList;
import java.util.List;

import com.kuronami.gravely.Gravely;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Gravely 自前のソウルバウンドエンチャント処理。
 *
 * 設計判断:
 *  - エンチャント本体は datapack-enchantment (data/gravely/enchantment/soulbound.json)、Java 不要
 *  - ApotheosisCompat と独立して動作 (異なる ResourceLocation)
 *  - 同じアイテムに両方付いてても、抽出された時点でリストから消えるので二重処理なし
 *  - Apotheosis 不在環境でも Gravely 単体で soulbound 体験を提供
 */
public final class GravelySoulboundHandler {

    private static final ResourceLocation SOULBOUND_ID =
            ResourceLocation.fromNamespaceAndPath(Gravely.MODID, "soulbound");

    private GravelySoulboundHandler() {}

    /**
     * drops から Gravely soulbound 付きアイテムを抜き、抜いた分は rescuedOut に追加する。
     * 死亡中の player に直接 add すると新 player object に引き継がれず消えるため、
     * 呼び出し側で PlayerRespawnEvent まで保留して、リスポーン後の player に追加する設計。
     *
     * @param rescuedOut 抜いたアイテムを追加する出力先 (DeathHandler の保留 list)
     * @return ソウルバウンドではないアイテムのリスト (これが墓に入る)
     */
    public static List<ItemStack> extractSoulbound(ServerPlayer player, List<ItemStack> drops, List<ItemStack> rescuedOut) {
        if (drops.isEmpty()) return drops;
        try {
            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, SOULBOUND_ID);
            Holder.Reference<Enchantment> soulbound = player.level().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(key);

            List<ItemStack> remaining = new ArrayList<>();
            int rescued = 0;
            for (ItemStack stack : drops) {
                if (stack.isEmpty()) continue;
                if (stack.getEnchantmentLevel(soulbound) > 0) {
                    rescuedOut.add(stack.copy());
                    rescued++;
                } else {
                    remaining.add(stack);
                }
            }
            if (rescued > 0) {
                Gravely.LOGGER.info("Gravely Soulbound: {} item(s) queued for {}'s respawn",
                        rescued, player.getName().getString());
            }
            return remaining;
        } catch (Throwable t) {
            // datapack 未ロード時等の lookup 失敗 → drops そのまま返す (安全 fallback)
            Gravely.LOGGER.warn("GravelySoulboundHandler.extractSoulbound failed: {}", t.toString());
            return drops;
        }
    }
}
