package com.agnes.survivalsplit.visual;

import com.agnes.survivalsplit.model.SplitMode;
import com.agnes.survivalsplit.model.SplitProfile;
import com.agnes.survivalsplit.store.ProfileStore;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record SyncPayload(int v, List<PlayerEntry> players) {

    public record PlayerEntry(String u, String m, boolean legacy) {
    }

    public static SyncPayload fromOnline(Collection<? extends Player> online, ProfileStore store,
                                         Set<UUID> legacyPlayers) {
        List<PlayerEntry> entries = new ArrayList<>();
        for (Player player : online) {
            SplitProfile profile = store.get(player.getUniqueId());
            if (profile != null) {
                boolean legacy = legacyPlayers.contains(player.getUniqueId()) || profile.legacy();
                entries.add(new PlayerEntry(player.getUniqueId().toString(), profile.mode().name(), legacy));
            }
        }
        return new SyncPayload(1, entries);
    }

    public static SyncPayload single(UUID uuid, SplitMode mode) {
        return single(uuid, mode, false);
    }

    public static SyncPayload single(UUID uuid, SplitMode mode, boolean legacy) {
        return new SyncPayload(1, List.of(new PlayerEntry(uuid.toString(), mode.name(), legacy)));
    }
}
