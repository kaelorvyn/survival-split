package com.agnes.survivalsplit.model;

public enum SplitPlatform {
    PC("电脑"),
    MOBILE("手机");

    private final String display;

    SplitPlatform(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }
}
