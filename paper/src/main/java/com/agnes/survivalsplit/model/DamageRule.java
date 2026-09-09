package com.agnes.survivalsplit.model;

public final class DamageRule {

    private DamageRule() {
    }

    /**
     * legacy（未装模组）玩家打不了和平玩家，但和平玩家可以打他；
     * legacy 与战斗/legacy 之间正常互伤。其余玩家：任一方为和平即取消。
     */
    public static boolean shouldCancel(SplitProfile victim, SplitProfile attacker) {
        if (victim == null || attacker == null) {
            victim = victim == null ? legacy() : victim;
            attacker = attacker == null ? legacy() : attacker;
        }
        if (victim.legacy() || attacker.legacy()) {
            if (victim.legacy()) {
                return false;
            }
            return victim.mode() == SplitMode.PEACEFUL;
        }
        return victim.mode() == SplitMode.PEACEFUL || attacker.mode() == SplitMode.PEACEFUL;
    }

    private static SplitProfile legacy() {
        return new SplitProfile(SplitMode.COMBAT, SplitPlatform.PC, true);
    }
}
