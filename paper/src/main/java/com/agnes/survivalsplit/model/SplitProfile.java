package com.agnes.survivalsplit.model;

public record SplitProfile(SplitMode mode, SplitPlatform platform, boolean legacy) {

    public SplitProfile(SplitMode mode, SplitPlatform platform) {
        this(mode, platform, false);
    }

    public String display() {
        return mode.display() + "玩家 + " + platform.display();
    }
}
