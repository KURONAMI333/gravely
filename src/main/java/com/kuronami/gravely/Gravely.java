package com.kuronami.gravely;

import com.kuronami.gravely.event.DeathHandler;
import com.kuronami.gravely.registry.GravelyBlockEntities;
import com.kuronami.gravely.registry.GravelyBlocks;
import com.kuronami.gravely.registry.GravelyDataComponents;
import com.kuronami.gravely.registry.GravelyItems;
import com.kuronami.gravely.registry.GravelyTabs;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

import org.slf4j.Logger;

@Mod(Gravely.MODID)
public class Gravely {
    public static final String MODID = "gravely";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Gravely(IEventBus modEventBus, ModContainer modContainer) {
        // 各種 DeferredRegister の登録
        GravelyBlocks.BLOCKS.register(modEventBus);
        GravelyItems.ITEMS.register(modEventBus);
        GravelyBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        GravelyDataComponents.COMPONENTS.register(modEventBus);

        // 墓ブロックをバニラ FUNCTIONAL_BLOCKS タブに追加
        modEventBus.addListener(GravelyTabs::addCreative);

        // ゲーム実行時イベント (LivingDeathEvent 等)
        NeoForge.EVENT_BUS.register(DeathHandler.class);

        // Config (テンプレ由来、後で gravely 専用に置き換える)
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 他のお墓 MOD との競合検出 (起動時 1 回ログ)
        com.kuronami.gravely.compat.GraveyardModConflict.logIfConflict();
    }
}
