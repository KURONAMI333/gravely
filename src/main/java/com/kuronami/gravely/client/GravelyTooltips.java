package com.kuronami.gravely.client;

import com.kuronami.gravely.Gravely;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Gravely 関連アイテムの追加 tooltip。
 *
 * 現状:
 *  - gravely:soulbound エンチャント付きの装備 / エンチャ本に説明文を追加
 */
@EventBusSubscriber(modid = Gravely.MODID, value = Dist.CLIENT)
public final class GravelyTooltips {

    private static final ResourceLocation SOULBOUND_ID =
            ResourceLocation.fromNamespaceAndPath(Gravely.MODID, "soulbound");

    private GravelyTooltips() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        // 装備に直接付与された enchantments / エンチャ本の stored_enchantments の両方を見る
        if (hasSoulbound(stack.get(DataComponents.ENCHANTMENTS))
                || hasSoulbound(stack.get(DataComponents.STORED_ENCHANTMENTS))) {
            event.getToolTip().add(
                    Component.translatable("enchantment.gravely.soulbound.desc")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
            );
        }
    }

    private static boolean hasSoulbound(ItemEnchantments enchants) {
        if (enchants == null) return false;
        for (Holder<Enchantment> h : enchants.keySet()) {
            if (h.unwrapKey().map(k -> k.location().equals(SOULBOUND_ID)).orElse(false)) {
                return true;
            }
        }
        return false;
    }
}
