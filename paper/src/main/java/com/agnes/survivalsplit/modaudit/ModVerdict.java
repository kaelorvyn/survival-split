package com.agnes.survivalsplit.modaudit;

public record ModVerdict(String mod, String verdict, double confidence,
                         String purpose, String reason) {

    public boolean isCheat() {
        return "CHEAT".equalsIgnoreCase(verdict);
    }
}
