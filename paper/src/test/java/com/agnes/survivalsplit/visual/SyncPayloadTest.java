package com.agnes.survivalsplit.visual;

import com.agnes.survivalsplit.model.SplitMode;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyncPayloadTest {

    private final Gson gson = new Gson();

    @Test
    void roundTripsThroughJson() {
        UUID uuid = UUID.randomUUID();
        SyncPayload original = SyncPayload.single(uuid, SplitMode.COMBAT);
        String json = gson.toJson(original);

        SyncPayload parsed = gson.fromJson(json, SyncPayload.class);

        assertEquals(1, parsed.v());
        assertEquals(uuid.toString(), parsed.players().get(0).u());
        assertEquals("COMBAT", parsed.players().get(0).m());
        assertEquals(false, parsed.players().get(0).legacy());
    }
}
