package com.agnes.survivalsplitclient;

import java.util.UUID;

public final class RenderAlphaContext {

    private static boolean active;

    private RenderAlphaContext() {
    }

    public static void begin(int entityId) {
        UUID uuid = EntityIdIndex.get(entityId);
        active = uuid != null && FactionStore.INSTANCE.isCrossFaction(uuid);
    }

    public static boolean isActive() {
        return active;
    }

    public static void end() {
        active = false;
    }
}
