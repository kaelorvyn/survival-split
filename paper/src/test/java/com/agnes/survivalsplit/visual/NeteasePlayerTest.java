package com.agnes.survivalsplit.visual;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeteasePlayerTest {

    @Test
    void onlyGeneratedNeteaseNamesAreClassifiedAsLegacy() {
        assertTrue(NeteaseNameRules.isGeneratedName("Netease1"));
        assertTrue(NeteaseNameRules.isGeneratedName("netease42"));
        assertFalse(NeteaseNameRules.isGeneratedName("Steve123"));
        assertFalse(NeteaseNameRules.isGeneratedName("Netease"));
        assertFalse(NeteaseNameRules.isGeneratedName("Guk很皮"));
    }
}
