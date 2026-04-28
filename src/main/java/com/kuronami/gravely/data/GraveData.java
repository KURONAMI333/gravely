package com.kuronami.gravely.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * 墓 BlockEntity が保持する所有者・アイテム・XP・タイムスタンプ情報。
 *
 * <p>1.21 の DataComponent として登録するため、Codec / StreamCodec を持つ record。
 *
 * @param ownerUuid       死亡したプレイヤーの UUID
 * @param ownerName       表示用のプレイヤー名 (オフライン時の表示・コマンドで使用)
 * @param items           墓に保管されたアイテム群 (vanilla inventory + 装備品 + 拡張スロット統合)
 * @param totalXp         保管された総 XP (Config.STORE_XP=true の時に使用)
 * @param deathTimestampMs 死亡時刻 (System.currentTimeMillis())
 */
public record GraveData(
        UUID ownerUuid,
        String ownerName,
        List<ItemStack> items,
        int totalXp,
        long deathTimestampMs
) {
    public static final GraveData EMPTY = new GraveData(
            new UUID(0L, 0L), "", List.of(), 0, 0L
    );

    public static final Codec<GraveData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    UUIDUtil.CODEC.fieldOf("owner").forGetter(GraveData::ownerUuid),
                    Codec.STRING.optionalFieldOf("owner_name", "").forGetter(GraveData::ownerName),
                    ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(GraveData::items),
                    Codec.INT.optionalFieldOf("xp", 0).forGetter(GraveData::totalXp),
                    Codec.LONG.optionalFieldOf("timestamp", 0L).forGetter(GraveData::deathTimestampMs)
            ).apply(instance, GraveData::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, GraveData> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, GraveData::ownerUuid,
                    ByteBufCodecs.STRING_UTF8, GraveData::ownerName,
                    ItemStack.OPTIONAL_LIST_STREAM_CODEC, GraveData::items,
                    ByteBufCodecs.VAR_INT, GraveData::totalXp,
                    ByteBufCodecs.VAR_LONG, GraveData::deathTimestampMs,
                    GraveData::new
            );

    public boolean isOwner(UUID candidate) {
        return candidate != null && candidate.equals(ownerUuid);
    }

    public boolean isEmpty() {
        return items.isEmpty() && totalXp <= 0;
    }

    /** items から null/EMPTY を除外したクリーンなリストを返す */
    public List<ItemStack> nonEmptyItems() {
        return items.stream().filter(s -> s != null && !s.isEmpty()).toList();
    }

    public Optional<ItemStack> firstItem() {
        return nonEmptyItems().stream().findFirst();
    }

    /** 死亡からの経過秒 */
    public long ageSeconds() {
        if (deathTimestampMs <= 0L) return 0L;
        return Math.max(0L, (System.currentTimeMillis() - deathTimestampMs) / 1000L);
    }
}
