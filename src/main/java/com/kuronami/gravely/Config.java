package com.kuronami.gravely;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Gravely の COMMON 設定。
 * 大半のお墓挙動はサーバー側で決まるので COMMON にまとめる。
 *
 * 設定項目はすべて hot-reload せず、サーバー再起動・ワールドリロード時に適用される想定。
 */
public final class Config {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    // ─────────────────────────────────────────
    // 設置位置探索
    // ─────────────────────────────────────────
    public static final ModConfigSpec.IntValue VERTICAL_SEARCH = B
            .comment(
                    "Maximum vertical distance to search for a safe grave position.",
                    "Y is searched both up and down from the death point."
            )
            .defineInRange("placement.verticalSearch", 16, 1, 256);

    public static final ModConfigSpec.IntValue HORIZONTAL_SEARCH = B
            .comment(
                    "Maximum horizontal radius (XZ) to search for a safe grave position.",
                    "Used as fallback when the death column has no safe spot."
            )
            .defineInRange("placement.horizontalSearch", 4, 0, 32);

    public static final ModConfigSpec.IntValue VOID_FALLBACK_Y = B
            .comment(
                    "When dying in the void or below this Y, the grave will be placed at this Y instead.",
                    "Set to a sensible build height like 64 (overworld) or your dimension default."
            )
            .defineInRange("placement.voidFallbackY", 64, -64, 320);

    public static final ModConfigSpec.BooleanValue ALLOW_PLACE_IN_FLUID = B
            .comment("If true, graves can be placed inside water (waterlogged-style). Lava is always avoided.")
            .define("placement.allowFluid", true);

    // ─────────────────────────────────────────
    // 所有者保護
    // ─────────────────────────────────────────
    public static final ModConfigSpec.EnumValue<ProtectionMode> PROTECTION_MODE = B
            .comment(
                    "Grave protection policy:",
                    "  OWNER_ONLY: only the owner can ever open the grave (PvE-friendly).",
                    "  TIMED:       only the owner can open during PROTECTION_DURATION_SEC, then anyone can.",
                    "  NONE:        anyone can open immediately (vanilla chest-like)."
            )
            .defineEnum("protection.mode", ProtectionMode.OWNER_ONLY);

    public static final ModConfigSpec.IntValue PROTECTION_DURATION_SEC = B
            .comment(
                    "When protection.mode = TIMED, the grave is owner-locked for this many seconds.",
                    "After this duration, any player can open it."
            )
            .defineInRange("protection.timedDurationSec", 60 * 60, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue OP_CAN_BYPASS = B
            .comment("Server operators (op level >= 2) can always open any grave.")
            .define("protection.opCanBypass", true);

    // ─────────────────────────────────────────
    // 通知
    // ─────────────────────────────────────────
    public static final ModConfigSpec.BooleanValue NOTIFY_GRAVE_CREATED = B
            .comment("Send a chat message to the player when their grave is created.")
            .define("notification.graveCreated", true);

    // ─────────────────────────────────────────
    // 機能 ON/OFF
    // ─────────────────────────────────────────
    public static final ModConfigSpec.BooleanValue ENABLE_GRAVES = B
            .comment("Master switch. If false, no graves are created (drops happen normally).")
            .define("feature.enabled", true);

    public static final ModConfigSpec.BooleanValue STORE_XP = B
            .comment("Store the player's experience inside the grave so it can be recovered too.")
            .define("feature.storeXp", true);

    public static final ModConfigSpec.BooleanValue REMOVE_BLOCK_ON_RECOVER = B
            .comment("Remove the grave block after the owner recovers its contents.")
            .define("feature.removeBlockOnRecover", true);

    static final ModConfigSpec SPEC = B.build();

    private Config() {}

    /** 保護モード */
    public enum ProtectionMode {
        OWNER_ONLY,
        TIMED,
        NONE
    }
}
