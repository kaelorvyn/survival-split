package com.agnes.survivalsplit.modaudit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModAuditFingerprintTest {

    private static final List<ModInfo> MODS = List.of(
            new ModInfo("fabric-api", "Fabric API", "0.141.6", "api"),
            new ModInfo("sodium", "Sodium", "0.6.0", "render"),
            new ModInfo("wurst", "Wurst", "7.54.1", "hack")
    );

    @Test
    void fingerprintIsStableRegardlessOfOrder() {
        List<ModInfo> reversed = List.of(
                MODS.get(2), MODS.get(0), MODS.get(1)
        );
        assertEquals(ModAuditFingerprint.fingerprint(MODS),
                ModAuditFingerprint.fingerprint(reversed));
    }

    @Test
    void fingerprintChangesWhenVersionChanges() {
        List<ModInfo> changed = List.of(
                new ModInfo("sodium", "Sodium", "0.7.0", "render"),
                new ModInfo("fabric-api", "Fabric API", "0.141.6", "api"),
                new ModInfo("wurst", "Wurst", "7.54.1", "hack")
        );
        assertTrue(!ModAuditFingerprint.fingerprint(MODS)
                .equals(ModAuditFingerprint.fingerprint(changed)));
    }

    @Test
    void changedOnlyFindsNewOrUpdatedMods() {
        List<ModInfo> previous = List.of(MODS.get(0), MODS.get(1));
        List<ModInfo> changed = ModAuditFingerprint.changedOnly(MODS, previous);
        assertEquals(1, changed.size());
        assertEquals("wurst", changed.get(0).id());
    }
}
