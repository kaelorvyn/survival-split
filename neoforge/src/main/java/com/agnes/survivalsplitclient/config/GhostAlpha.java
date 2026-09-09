package com.agnes.survivalsplitclient.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reads the client-only opacity setting without requiring a config library. */
public final class GhostAlpha {
    private static final int DEFAULT_OPACITY = 35;
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("survivalsplit-client.json");
    private static volatile long loadedTimestamp = Long.MIN_VALUE;
    private static volatile int loadedAlpha = opacityToAlpha(DEFAULT_OPACITY);

    private GhostAlpha() {
    }

    public static int alpha() {
        try {
            ensureFile();
            long timestamp = Files.getLastModifiedTime(FILE).toMillis();
            if (timestamp != loadedTimestamp) {
                loadedAlpha = readAlpha();
                loadedTimestamp = timestamp;
            }
        } catch (IOException | RuntimeException ignored) {
            loadedAlpha = opacityToAlpha(DEFAULT_OPACITY);
        }
        return loadedAlpha;
    }

    private static void ensureFile() throws IOException {
        if (Files.exists(FILE)) {
            return;
        }
        Files.createDirectories(FILE.getParent());
        Files.writeString(FILE, "{\n  \"ghostOpacity\": 35\n}\n", StandardCharsets.UTF_8);
    }

    private static int readAlpha() throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
        int opacity = root.has("ghostOpacity") ? root.get("ghostOpacity").getAsInt() : DEFAULT_OPACITY;
        return opacityToAlpha(Math.max(0, Math.min(100, opacity)));
    }

    private static int opacityToAlpha(int opacity) {
        return Math.round(opacity * 255f / 100f);
    }
}
