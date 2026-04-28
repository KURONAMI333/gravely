package com.kuronami.gravely;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * クライアント限定の初期化。NeoForge の inner-mod パターンで分離。
 * 専用画面 (ConfigurationScreen 自動生成) を登録する。
 */
@Mod(value = Gravely.MODID, dist = Dist.CLIENT)
public class GravelyClient {
    public GravelyClient(ModContainer container) {
        // Mods 画面 → Gravely → Config をクリックすると自動生成された設定画面が開く
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
