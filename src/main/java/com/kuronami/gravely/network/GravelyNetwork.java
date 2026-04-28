package com.kuronami.gravely.network;

import com.kuronami.gravely.Gravely;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge 1.21 のカスタムパケット登録。
 *
 * 現状: 墓生成位置をクライアントへ送信する payload (JourneyMap waypoint 登録用)
 */
@EventBusSubscriber(modid = Gravely.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class GravelyNetwork {

    private GravelyNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        // optional() = クライアント側にこの payload type が未登録でも切断しない
        // (将来 Gravely を server-side のみ前提で運用する可能性に備える)
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(
                GravePosPayload.TYPE,
                GravePosPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.kuronami.gravely.compat.jm.JourneyMapClientHook.onGravePos(payload)
                )
        );
        registrar.playToClient(
                GraveRecoveredPayload.TYPE,
                GraveRecoveredPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        com.kuronami.gravely.compat.jm.JourneyMapClientHook.onGraveRecovered(payload)
                )
        );
    }
}
