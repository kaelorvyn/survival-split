package com.agnes.survivalsplitclient;

import java.util.Locale;

public final class PlatformDetector {
    private PlatformDetector() {
    }

    public static String detect() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String vendor = System.getProperty("java.vendor", "").toLowerCase(Locale.ROOT);
        String vm = System.getProperty("java.vm.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (os.contains("android") || vendor.contains("pojav") || vendor.contains("fcl")
                || vm.contains("pojav") || vm.contains("fcl")
                || (os.contains("linux") && (arch.contains("aarch64") || arch.contains("arm")))) {
            return "MOBILE";
        }
        if (os.contains("windows") || os.contains("linux") || os.contains("mac")) {
            return "PC";
        }
        return "UNKNOWN";
    }
}
