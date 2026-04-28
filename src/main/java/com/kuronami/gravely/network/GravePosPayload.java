package com.kuronami.gravely.network;

import com.kuronami.gravely.Gravely;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * 墓生成時にサーバ→クライアントへ送信される位置情報 payload。
 * クライアント側で JourneyMap への waypoint 登録に利用される。
 */
public record GravePosPayload(BlockPos pos, ResourceKey<Level> dimension, String ownerName)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GravePosPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Gravely.MODID, "grave_pos"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GravePosPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, GravePosPayload::pos,
                    ResourceKey.streamCodec(Registries.DIMENSION), GravePosPayload::dimension,
                    ByteBufCodecs.STRING_UTF8, GravePosPayload::ownerName,
                    GravePosPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
