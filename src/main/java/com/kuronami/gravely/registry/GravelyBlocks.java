package com.kuronami.gravely.registry;

import com.kuronami.gravely.Gravely;
import com.kuronami.gravely.block.GraveBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GravelyBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Gravely.MODID);

    public static final DeferredBlock<GraveBlock> GRAVE = BLOCKS.register(
            "grave",
            () -> new GraveBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(-1.0F, 3600000.0F)              // 破壊不可、爆発耐性 MAX (所有者以外は壊せない設計)
                    .sound(SoundType.STONE)
                    .pushReaction(PushReaction.BLOCK)         // ピストンで動かない
                    .noOcclusion()                            // 半透明な部分を許可
            )
    );

    private GravelyBlocks() {}
}
