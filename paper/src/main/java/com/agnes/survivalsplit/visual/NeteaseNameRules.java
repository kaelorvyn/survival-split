package com.agnes.survivalsplit.visual;

import java.util.regex.Pattern;

final class NeteaseNameRules {

    private static final Pattern GENERATED_NAME = Pattern.compile("Netease[0-9]+", Pattern.CASE_INSENSITIVE);

    private NeteaseNameRules() {
    }

    static boolean isGeneratedName(String name) {
        return name != null && GENERATED_NAME.matcher(name).matches();
    }
}
