package com.agnes.survivalsplit.modaudit;

import com.google.gson.Gson;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class ModAuditStore {

    public record Entry(String fingerprint, long lastSeen, List<ModInfo> mods) {
    }

    private static final Gson GSON = new Gson();

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Entry> entries = new HashMap<>();

    public ModAuditStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "mod-audit.yml");
        load();
    }

    public Entry get(UUID uuid) {
        return entries.get(uuid);
    }

    public void save(UUID uuid, String fingerprint, List<ModInfo> mods) {
        entries.put(uuid, new Entry(fingerprint, System.currentTimeMillis(), List.copyOf(mods)));
        persist();
    }

    public void touch(UUID uuid) {
        Entry entry = entries.get(uuid);
        if (entry == null) {
            return;
        }
        entries.put(uuid, new Entry(entry.fingerprint(), System.currentTimeMillis(), entry.mods()));
        persist();
    }

    private void load() {
        entries.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String fingerprint = yaml.getString(key + ".fingerprint", "");
                long lastSeen = yaml.getLong(key + ".last-seen", 0);
                String json = yaml.getString(key + ".mods", "[]");
                ModInfo[] mods = GSON.fromJson(json, ModInfo[].class);
                if (mods == null) {
                    continue;
                }
                entries.put(uuid, new Entry(fingerprint, lastSeen, List.of(mods)));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("跳过无效模组审计条目: " + key);
            }
        }
    }

    private void persist() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Entry> entry : entries.entrySet()) {
            String key = entry.getKey().toString();
            yaml.set(key + ".fingerprint", entry.getValue().fingerprint());
            yaml.set(key + ".last-seen", entry.getValue().lastSeen());
            yaml.set(key + ".mods", GSON.toJson(entry.getValue().mods()));
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "无法保存 mod-audit.yml", e);
        }
    }
}
