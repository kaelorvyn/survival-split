package com.agnes.survivalsplitclient;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EntityIdIndex {
    private static final Map<Integer, UUID> BY_ID = new ConcurrentHashMap<>();
    private EntityIdIndex() {
    }
    public static void put(int entityId, UUID uuid) { BY_ID.put(entityId, uuid); }
    public static UUID get(int entityId) { return BY_ID.get(entityId); }
    public static void clear() { BY_ID.clear(); }
}
