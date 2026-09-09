package com.agnes.survivalsplitclient;

import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

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
        var players = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("players");
        for (var element : players) {
            var entry = element.getAsJsonObject();
            next.put(UUID.fromString(entry.get("u").getAsString()),
                    new FactionInfo(entry.get("m").getAsString(),
                            entry.has("legacy") && entry.get("legacy").getAsBoolean()));
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
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || target == null || target.equals(client.player.getUUID())) return false;
        synchronized (factions) {
            FactionInfo mine = factions.get(client.player.getUUID());
            FactionInfo theirs = factions.get(target);
            return mine != null && theirs != null && !mine.legacy() && !theirs.legacy()
                    && !mine.mode().equals(theirs.mode());
        }
    }

    private record FactionInfo(String mode, boolean legacy) {
    }
}
