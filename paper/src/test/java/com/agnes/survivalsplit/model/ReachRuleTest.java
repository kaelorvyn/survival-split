package com.agnes.survivalsplit.model;

import com.agnes.survivalsplit.model.ReachRule.ReachValues;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReachRuleTest {

    @Test
    void pcAndPeacefulUseDefaultRange() {
        ReachValues pc = ReachRule.defaults();
        assertEquals(4.5, ReachRule.forProfile(new SplitProfile(SplitMode.COMBAT, SplitPlatform.PC), pc, ReachRule.mobileCombatDefaults()).blockRange());
        assertEquals(3.0, ReachRule.forProfile(new SplitProfile(SplitMode.PEACEFUL, SplitPlatform.MOBILE), pc, ReachRule.mobileCombatDefaults()).entityRange());
        assertEquals(4.5, ReachRule.forProfile(null, pc, ReachRule.mobileCombatDefaults()).blockRange());
    }

    @Test
    void mobileCombatGetsOneExtraBlock() {
        ReachValues values = ReachRule.forProfile(
                new SplitProfile(SplitMode.COMBAT, SplitPlatform.MOBILE),
                ReachRule.defaults(),
                ReachRule.mobileCombatDefaults());
        assertEquals(5.5, values.blockRange());
        assertEquals(4.0, values.entityRange());
    }
}
