package com.kuronami.gravely.registry;

import com.kuronami.gravely.Gravely;

import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GravelyItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Gravely.MODID);

    public static final DeferredItem<BlockItem> GRAVE_ITEM =
            ITEMS.registerSimpleBlockItem("grave", GravelyBlocks.GRAVE);

    private GravelyItems() {}
}
