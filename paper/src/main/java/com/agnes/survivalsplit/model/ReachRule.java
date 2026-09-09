package com.agnes.survivalsplit.model;

public final class ReachRule {

    private ReachRule() {
    }

    public record ReachValues(double blockRange, double entityRange) {
    }

    public static ReachValues defaults() {
        return new ReachValues(4.5, 3.0);
    }

    public static ReachValues mobileCombatDefaults() {
        return new ReachValues(5.5, 4.0);
    }

    public static ReachValues forProfile(SplitProfile profile, ReachValues pc, ReachValues mobileCombat) {
        if (profile != null
                && profile.mode() == SplitMode.COMBAT
                && profile.platform() == SplitPlatform.MOBILE) {
            return mobileCombat;
        }
        return pc;
    }
}
