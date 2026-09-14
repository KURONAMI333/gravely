package com.kuronami.gravely.registry;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * Gravely のクリエイティブタブ統合。
 *
 * 設計判断 (2026-04-27):
 *  - 独自タブは作らない (墓ブロック1個しかない、独自タブは過剰)
 *  - バニラの FUNCTIONAL_BLOCKS タブに墓を追加するだけ
 *  - 将来アイテム/ブロック増えたら独自タブ復活を検討
 *  - NeoForge 1.21 の withTabsBefore/After は他MOD制約と衝突して順序が安定しない
 */
public final class GravelyTabs {

    private GravelyTabs() {}

    /** バニラ FUNCTIONAL_BLOCKS タブに墓を追加 */
    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(GravelyItems.GRAVE_ITEM);
        }
    }
}
