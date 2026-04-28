package com.kuronami.gravely.network;

import com.kuronami.gravely.Gravely;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * 墓回収時にサーバ→クライアントへ送信される削除通知 payload。
 * クライアント側で該当 waypoint を JourneyMap から削除する。
 */
public record GraveRecoveredPayload(BlockPos pos, ResourceKey<Level> dimension)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GraveRecoveredPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Gravely.MODID, "grave_recovered"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GraveRecoveredPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, GraveRecoveredPayload::pos,
                    ResourceKey.streamCodec(Registries.DIMENSION), GraveRecoveredPayload::dimension,
                    GraveRecoveredPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
