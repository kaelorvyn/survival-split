package com.agnes.survivalsplit;

import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class KaelorvynTitleBridge {

    public static void refresh(UUID uuid) {
        if (uuid == null) {
            return;
        }
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("KaelorvynTitle");
            if (plugin == null || !plugin.isEnabled()) {
                return;
            }
            Method refresh = plugin.getClass().getMethod("refreshPlayer", UUID.class);
            refresh.invoke(plugin, uuid);
        } catch (Throwable ignored) {
        }
    }

    private KaelorvynTitleBridge() {
    }
}
