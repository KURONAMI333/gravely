package com.kuronami.gravely.compat;

import java.util.ArrayList;
import java.util.List;

import com.kuronami.gravely.Gravely;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Curios MOD との連携 (薄いゲートウェイ層)。
 *
 * 設計のポイント:
 *  - 静的メソッドだけが {@code CuriosApi} を **参照しない** ように書く。
 *  - 実際に CuriosApi を呼ぶコードは、private static class {@code Inner} に閉じ込める。
 *  - JVM のクラスロード遅延性のおかげで、Curios が居ない環境では Inner はロードされない
 *    → ClassNotFoundError が起きない。
 *
 * 同じ書き方は他MOD連携 (Trinkets/Accessories/Backpacked等) でも再利用予定。
 */
public final class CuriosCompat {

    private CuriosCompat() {}

    public static boolean isLoaded() {
        return ModList.get() != null && ModList.get().isLoaded("curios");
    }

    /**
     * プレイヤーの全 Curios slot からアイテムを抽出し、Curios インベントリを空にする。
     * Curios 不在 → 空リスト。
     */
    public static List<ItemStack> collectAndClearItems(Player player) {
        if (!isLoaded()) return List.of();
        try {
            return Inner.collectAndClearItems(player);
        } catch (Throwable t) {
            Gravely.LOGGER.warn("CuriosCompat.collectAndClearItems failed: {}", t.toString());
            return List.of();
        }
    }

    /**
     * 取り出した Curios アイテムを再装着試行。入りきらないアイテムを返す (呼び出し側で通常インベントリに)。
     */
    public static List<ItemStack> tryEquipBack(Player player, List<ItemStack> items) {
        if (!isLoaded()) return new ArrayList<>(items);
        if (items.isEmpty()) return List.of();
        try {
            return Inner.tryEquipBack(player, items);
        } catch (Throwable t) {
            Gravely.LOGGER.warn("CuriosCompat.tryEquipBack failed: {}", t.toString());
            return new ArrayList<>(items);
        }
    }

    // ─────────────────────────────────────────────────────
    // Curios クラス参照は **この Inner だけ**。Curios 不在ではロードされない。
    // ─────────────────────────────────────────────────────
    private static final class Inner {
        static List<ItemStack> collectAndClearItems(Player player) {
            return top.theillusivec4.curios.api.CuriosApi
                    .getCuriosInventory(player)
                    .map(handler -> {
                        List<ItemStack> out = new ArrayList<>();
                        for (var entry : handler.getCurios().entrySet()) {
                            var stacks = entry.getValue();
                            for (int i = 0; i < stacks.getSlots(); i++) {
                                ItemStack s = stacks.getStacks().getStackInSlot(i);
                                if (!s.isEmpty()) {
                                    out.add(s.copy());
                                    stacks.getStacks().setStackInSlot(i, ItemStack.EMPTY);
                                }
                                ItemStack c = stacks.getCosmeticStacks().getStackInSlot(i);
                                if (!c.isEmpty()) {
                                    out.add(c.copy());
                                    stacks.getCosmeticStacks().setStackInSlot(i, ItemStack.EMPTY);
                                }
                            }
                        }
                        return out;
                    })
                    .orElse(List.of());
        }

        static List<ItemStack> tryEquipBack(Player player, List<ItemStack> items) {
            var handler = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).orElse(null);
            if (handler == null) return new ArrayList<>(items);
            List<ItemStack> remaining = new ArrayList<>();
            for (ItemStack stack : items) {
                if (stack.isEmpty()) continue;
                boolean placed = false;
                outer:
                for (var entry : handler.getCurios().entrySet()) {
                    var stacks = entry.getValue();
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        if (stacks.getStacks().getStackInSlot(i).isEmpty()
                                && top.theillusivec4.curios.api.CuriosApi.isStackValid(
                                        new top.theillusivec4.curios.api.SlotContext(
                                                entry.getKey(), player, i, false, true),
                                        stack)) {
                            stacks.getStacks().setStackInSlot(i, stack.copy());
                            placed = true;
                            break outer;
                        }
                    }
                }
                if (!placed) {
                    remaining.add(stack);
                }
            }
            return remaining;
        }
    }
}
