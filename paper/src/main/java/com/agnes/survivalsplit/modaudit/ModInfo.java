package com.agnes.survivalsplit.modaudit;

public record ModInfo(String id, String name, String version, String description) {

    public String key() {
        return id() + "|" + version();
    }
}
