package com.agnes.survivalsplit.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageRuleTest {

    private static final SplitProfile PEACEFUL = new SplitProfile(SplitMode.PEACEFUL, SplitPlatform.PC);
    private static final SplitProfile COMBAT = new SplitProfile(SplitMode.COMBAT, SplitPlatform.PC);
    private static final SplitProfile LEGACY = new SplitProfile(SplitMode.COMBAT, SplitPlatform.PC, true);

    @Test
    void combatVsCombatIsAllowed() {
        assertFalse(DamageRule.shouldCancel(COMBAT, COMBAT));
    }

    @Test
    void anyPeacefulSideIsCancelled() {
        assertTrue(DamageRule.shouldCancel(PEACEFUL, PEACEFUL));
        assertTrue(DamageRule.shouldCancel(PEACEFUL, COMBAT));
        assertTrue(DamageRule.shouldCancel(COMBAT, PEACEFUL));
    }

    @Test
    void peacefulCanHitLegacyButLegacyCannotHitPeaceful() {
        assertTrue(DamageRule.shouldCancel(PEACEFUL, LEGACY));
        assertFalse(DamageRule.shouldCancel(LEGACY, PEACEFUL));
        assertFalse(DamageRule.shouldCancel(LEGACY, COMBAT));
        assertFalse(DamageRule.shouldCancel(COMBAT, LEGACY));
        assertFalse(DamageRule.shouldCancel(LEGACY, LEGACY));
    }

    @Test
    void unknownProfileIsTreatedAsLegacy() {
        assertFalse(DamageRule.shouldCancel(null, PEACEFUL));
        assertFalse(DamageRule.shouldCancel(COMBAT, null));
        assertTrue(DamageRule.shouldCancel(PEACEFUL, null));
    }
}
