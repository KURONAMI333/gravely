package com.kuronami.gravely.compat.jm;

import com.kuronami.gravely.Gravely;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.event.ClientEventRegistry;

/**
 * JourneyMap が起動時に検出して呼び出すプラグインエントリ。
 *
 * 注意:
 *  - {@code @JourneyMapPlugin} が付いているので JourneyMap が自動検出する
 *  - JourneyMap 不在環境ではこのクラス自体がロードされない (誰も参照しない + JM が探さない)
 *    → NoClassDefFoundError は起きない
 *  - {@link #api} は static で JourneyMapClientHook から参照される
 */
@JourneyMapPlugin(apiVersion = IClientAPI.API_VERSION)
public class GravelyJourneyMapPlugin implements IClientPlugin {

    /** JourneyMap が initialize() で渡してくる API ハンドル */
    public static volatile IClientAPI api;

    @Override
    public void initialize(IClientAPI jmClientApi) {
        api = jmClientApi;

        // JM 標準の自動 death waypoint を抑制 (Gravely 自身の墓 waypoint と被るため)
        ClientEventRegistry.DEATH_WAYPOINT_EVENT.subscribe(Gravely.MODID, event -> {
            event.cancel();
            Gravely.LOGGER.debug("Cancelled JourneyMap auto death waypoint at {}", event.location);
        });

        Gravely.LOGGER.info("JourneyMap API initialized for Gravely (auto-death-waypoint suppressed)");
    }

    @Override
    public String getModId() {
        return Gravely.MODID;
    }
}
