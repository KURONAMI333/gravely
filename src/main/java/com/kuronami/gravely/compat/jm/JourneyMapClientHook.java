package com.kuronami.gravely.compat.jm;

import com.kuronami.gravely.Gravely;
import com.kuronami.gravely.network.GravePosPayload;

import net.neoforged.fml.ModList;

/**
 * JourneyMap 連携のクライアント側ゲートウェイ。
 *
 * 設計:
 *  - JM 不在時に NoClassDefFoundError を起こさないため、JM API 参照は Inner class に閉じ込める
 *  - サーバから {@link GravePosPayload} を受信したクライアントが呼ぶ
 *  - {@link GravelyJourneyMapPlugin#api} に保持された IClientAPI 経由で waypoint 登録
 */
public final class JourneyMapClientHook {

    private JourneyMapClientHook() {}

    public static boolean isJourneyMapLoaded() {
        return ModList.get() != null && ModList.get().isLoaded("journeymap");
    }

    public static void onGravePos(GravePosPayload payload) {
        if (!isJourneyMapLoaded()) return;
        try {
            Inner.show(payload);
        } catch (Throwable t) {
            Gravely.LOGGER.warn("JourneyMap waypoint show failed: {}", t.toString());
        }
    }

    public static void onGraveRecovered(com.kuronami.gravely.network.GraveRecoveredPayload payload) {
        if (!isJourneyMapLoaded()) return;
        try {
            Inner.remove(payload);
        } catch (Throwable t) {
            Gravely.LOGGER.warn("JourneyMap waypoint remove failed: {}", t.toString());
        }
    }

    /** Gravely ブランド紫 (回収演出の魔法光と統一) */
    private static final int GRAVELY_WAYPOINT_COLOR = 0x8B5CF6;

    /** JM API 参照はここだけ。JM 不在時はクラスロードされない */
    private static final class Inner {
        static void show(GravePosPayload payload) {
            journeymap.api.v2.client.IClientAPI api = GravelyJourneyMapPlugin.api;
            if (api == null) {
                Gravely.LOGGER.debug("JourneyMap API not yet initialized, skipping waypoint");
                return;
            }
            String name = payload.ownerName() + "の墓";
            journeymap.api.v2.common.waypoint.Waypoint wp =
                    journeymap.api.v2.common.waypoint.WaypointFactory.createClientWaypoint(
                            Gravely.MODID,
                            payload.pos(),
                            name,
                            payload.dimension(),
                            true   // persistent: 次回起動でも残る
                    );
            wp.setColor(GRAVELY_WAYPOINT_COLOR);
            api.addWaypoint(Gravely.MODID, wp);
            Gravely.LOGGER.info("JourneyMap waypoint registered: {} @ {}", name, payload.pos());
        }

        static void remove(com.kuronami.gravely.network.GraveRecoveredPayload payload) {
            journeymap.api.v2.client.IClientAPI api = GravelyJourneyMapPlugin.api;
            if (api == null) return;
            // pos と dim 一致する waypoint を全て削除 (まれに同位置に複数あっても安全)
            int removed = 0;
            for (journeymap.api.v2.common.waypoint.Waypoint wp :
                    api.getAllWaypoints(payload.dimension())) {
                if (!Gravely.MODID.equals(wp.getModId())) continue;
                if (wp.getBlockPos().equals(payload.pos())) {
                    api.removeWaypoint(Gravely.MODID, wp);
                    removed++;
                }
            }
            if (removed > 0) {
                Gravely.LOGGER.info("JourneyMap waypoint removed: {} @ {}", removed, payload.pos());
            }
        }
    }
}
