package com.agnes.survivalsplit.modaudit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ModAuditFingerprint {

    private ModAuditFingerprint() {
    }

    public static String fingerprint(List<ModInfo> mods) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            List<String> lines = mods.stream()
                    .map(ModInfo::key)
                    .distinct()
                    .sorted()
                    .toList();
            for (String line : lines) {
                digest.update(line.getBytes(StandardCharsets.UTF_8));
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new IllegalStateException("无法计算模组指纹", e);
        }
    }

    public static List<ModInfo> changedOnly(List<ModInfo> current, List<ModInfo> previous) {
        if (previous == null || previous.isEmpty()) {
            return List.copyOf(current);
        }
        Set<String> known = new HashSet<>();
        for (ModInfo mod : previous) {
            known.add(mod.key());
        }
        List<ModInfo> changed = new ArrayList<>();
        for (ModInfo mod : current) {
            if (!known.contains(mod.key())) {
                changed.add(mod);
            }
        }
        return List.copyOf(changed);
    }

    public static String truncate(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
