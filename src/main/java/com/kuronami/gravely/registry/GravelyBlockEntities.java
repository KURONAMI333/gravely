package com.kuronami.gravely.registry;

import com.kuronami.gravely.Gravely;
import com.kuronami.gravely.block.GraveBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GravelyBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Gravely.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GraveBlockEntity>> GRAVE =
            BLOCK_ENTITIES.register(
                    "grave",
                    () -> BlockEntityType.Builder.of(GraveBlockEntity::new, GravelyBlocks.GRAVE.get()).build(null)
            );

    private GravelyBlockEntities() {}
}
