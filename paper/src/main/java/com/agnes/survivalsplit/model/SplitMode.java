package com.agnes.survivalsplit.model;

public enum SplitMode {
    PEACEFUL("和平"),
    COMBAT("战斗");

    private final String display;

    SplitMode(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }
}
