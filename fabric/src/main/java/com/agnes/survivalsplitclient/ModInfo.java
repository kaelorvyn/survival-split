package com.agnes.survivalsplitclient;

import net.fabricmc.loader.api.ModContainer;

public record ModInfo(String id, String name, String version, String description) {

    public static ModInfo from(ModContainer container) {
        var meta = container.getMetadata();
        String description = meta.getDescription() == null ? "" : meta.getDescription();
        if (description.length() > 200) {
            description = description.substring(0, 200);
        }
        return new ModInfo(meta.getId(), meta.getName(),
                meta.getVersion().getFriendlyString(), description);
    }
}
