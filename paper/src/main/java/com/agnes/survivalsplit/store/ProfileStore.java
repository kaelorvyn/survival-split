package com.agnes.survivalsplit.store;

import com.agnes.survivalsplit.model.SplitMode;
import com.agnes.survivalsplit.model.SplitPlatform;
import com.agnes.survivalsplit.model.SplitProfile;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class ProfileStore {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, SplitProfile> profiles = new HashMap<>();

    public ProfileStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "profiles.yml");
        load();
    }

    public void load() {
        profiles.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                SplitMode mode = SplitMode.valueOf(yaml.getString(key + ".mode", ""));
                SplitPlatform platform = SplitPlatform.valueOf(yaml.getString(key + ".platform", ""));
                boolean legacy = yaml.getBoolean(key + ".legacy", false);
                profiles.put(uuid, new SplitProfile(mode, platform, legacy));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("跳过无效档案条目: " + key);
            }
        }
    }

    public SplitProfile get(UUID uuid) {
        return profiles.get(uuid);
    }

    public void set(UUID uuid, SplitProfile profile) {
        profiles.put(uuid, profile);
        save();
    }

    public Map<UUID, SplitProfile> all() {
        return Collections.unmodifiableMap(profiles);
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, SplitProfile> entry : profiles.entrySet()) {
            String key = entry.getKey().toString();
            yaml.set(key + ".mode", entry.getValue().mode().name());
            yaml.set(key + ".platform", entry.getValue().platform().name());
            yaml.set(key + ".legacy", entry.getValue().legacy());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "无法保存 profiles.yml", e);
        }
    }
}
