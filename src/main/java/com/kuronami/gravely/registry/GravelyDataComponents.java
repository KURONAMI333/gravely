package com.kuronami.gravely.registry;

import com.kuronami.gravely.Gravely;
import com.kuronami.gravely.data.GraveData;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GravelyDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Gravely.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GraveData>> GRAVE_DATA =
            COMPONENTS.registerComponentType(
                    "grave_data",
                    builder -> builder
                            .persistent(GraveData.CODEC)
                            .networkSynchronized(GraveData.STREAM_CODEC)
            );

    private GravelyDataComponents() {}
}
