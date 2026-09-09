package com.agnes.survivalsplit.listener;

import com.agnes.survivalsplit.model.ReachRule;
import com.agnes.survivalsplit.model.ReachRule.ReachValues;
import com.agnes.survivalsplit.model.SplitProfile;
import com.agnes.survivalsplit.store.ProfileStore;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class ReachManager {

    private ReachManager() {
    }

    public static void apply(Player player, ProfileStore store, ConfigurationSection config) {
        SplitProfile profile = store.get(player.getUniqueId());
        ReachValues pc = new ReachValues(
                config.getDouble("reach.pc-block", 4.5),
                config.getDouble("reach.pc-entity", 3.0));
        ReachValues mobileCombat = new ReachValues(
                config.getDouble("reach.mobile-combat-block", 5.5),
                config.getDouble("reach.mobile-combat-entity", 4.0));
        ReachValues values = ReachRule.forProfile(profile, pc, mobileCombat);

        setBase(player, Attribute.BLOCK_INTERACTION_RANGE, values.blockRange());
        setBase(player, Attribute.ENTITY_INTERACTION_RANGE, values.entityRange());
    }

    public static void applyAllOnline(Plugin plugin, ProfileStore store, ConfigurationSection config) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            apply(player, store, config);
        }
    }

    private static void setBase(Player player, Attribute attribute, double value) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }
}
