package com.agnes.survivalsplitclient.config;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.loader.api.FabricLoader;

public final class GhostAlpha {

    private GhostAlpha() {
    }

    public static int alpha() {
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            try {
                return AutoConfig.getConfigHolder(SplitConfig.class).get().alpha();
            } catch (Throwable ignored) {
                // 配置读取失败时使用默认 35%。
            }
        }
        return 89;
    }
}
