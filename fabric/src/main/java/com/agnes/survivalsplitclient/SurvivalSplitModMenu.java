package com.agnes.survivalsplitclient;

import com.agnes.survivalsplitclient.config.SplitConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.loader.api.FabricLoader;

public final class SurvivalSplitModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
                return parent;
            }
            return AutoConfig.getConfigScreen(SplitConfig.class, parent).get();
        };
    }
}
