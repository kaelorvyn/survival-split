package com.agnes.survivalsplitclient.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "survivalsplit-client")
public class SplitConfig implements ConfigData {

    /** 跨阵营虚化不透明度：0 完全不显示身体，100 完全显示。 */
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int ghostOpacity = 35;

    public int alpha() {
        return Math.round(ghostOpacity * 255f / 100f);
    }
}
