package com.kuronami.gravely.compat;

import java.util.List;
import java.util.Set;

import com.kuronami.gravely.Gravely;

import net.neoforged.fml.ModList;

/**
 * 他のお墓 MOD との競合検出。
 *
 * 設計判断 (2026-04-27):
 *  - Gravely 優先: 競合 MOD が居ても Gravely が drops を引き取る
 *  - LivingDropsEvent を priority=HIGHEST で受けて event.setCanceled(true) で他MODを抑制
 *  - 起動時 1 回ログ警告、初回死亡時にプレイヤーへ chat 通知
 */
public final class GraveyardModConflict {

    /** 死亡時にアイテム保管を行うお墓系 MOD ID 群 (機能競合する) */
    private static final Set<String> CONFLICTING_MOD_IDS = Set.of(
            "gravestone",
            "tombstone",
            "gravestone-mod-graves",
            "yigd",
            "universal_graves",
            "pneumono_gravestones",
            "ly-graves",
            "ly_graves",
            "soul_gravestone",
            "soulbound_gravestones",
            "carcass",
            "death_chest",
            "deathchest",
            "gravekeeper",
            "simplecorpse",
            "youritemsaresafe",
            "deathreimagined"
    );

    private static volatile List<String> cachedDetected = null;

    private GraveyardModConflict() {}

    public static List<String> detectConflicting() {
        if (cachedDetected != null) return cachedDetected;
        ModList list = ModList.get();
        if (list == null) return List.of();
        cachedDetected = CONFLICTING_MOD_IDS.stream()
                .filter(list::isLoaded)
                .toList();
        return cachedDetected;
    }

    public static boolean hasConflict() {
        return !detectConflicting().isEmpty();
    }

    /** 起動時に 1 回呼ばれる: 競合検出をログに警告 */
    public static void logIfConflict() {
        List<String> detected = detectConflicting();
        if (detected.isEmpty()) return;
        Gravely.LOGGER.warn("⚠ Gravely detected other grave-handling mods: {}. " +
                        "Gravely will take precedence (priority=HIGHEST + event cancel). " +
                        "Disable one of them to suppress this warning.",
                String.join(", ", detected));
    }
}
