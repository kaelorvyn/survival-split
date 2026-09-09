package com.agnes.survivalsplitclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FactionStore {

    public static final FactionStore INSTANCE = new FactionStore();

    private final Map<UUID, FactionInfo> factions = new HashMap<>();

    private FactionStore() {
    }

    public void loadJson(String json) {
        Map<UUID, FactionInfo> next = new HashMap<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray players = root.getAsJsonArray("players");
        for (JsonElement element : players) {
            JsonObject entry = element.getAsJsonObject();
            UUID uuid = UUID.fromString(entry.get("u").getAsString());
            boolean legacy = entry.has("legacy") && entry.get("legacy").getAsBoolean();
            next.put(uuid, new FactionInfo(entry.get("m").getAsString(), legacy));
        }
        synchronized (factions) {
            factions.clear();
            factions.putAll(next);
        }
    }

    public void clear() {
        synchronized (factions) {
            factions.clear();
        }
    }

    public boolean isCrossFaction(UUID target) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || target == null) {
            return false;
        }
        UUID self = client.player.getUuid();
        if (self.equals(target)) {
            return false;
        }
        synchronized (factions) {
            FactionInfo mine = factions.get(self);
            FactionInfo theirs = factions.get(target);
            if (mine == null || theirs == null) {
                return false;
            }
            if (mine.legacy() || theirs.legacy()) {
                return false;
            }
            return !mine.mode().equals(theirs.mode());
        }
    }

    private record FactionInfo(String mode, boolean legacy) {
    }
}
