package com.agnes.survivalsplitclient;

import com.agnes.survivalsplitclient.config.GhostAlpha;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Per-render-call state. Minecraft submits client rendering on one thread. */
public final class RenderAlphaContext {
    private static boolean active;
    private static ResourceLocation texture;

    private RenderAlphaContext() {
    }

    public static void begin(int entityId, ResourceLocation bodyTexture) {
        UUID uuid = EntityIdIndex.get(entityId);
        active = uuid != null && FactionStore.INSTANCE.isCrossFaction(uuid);
        texture = bodyTexture;
    }

    public static boolean isActive() {
        return active;
    }

    public static RenderType bodyRenderType(RenderType original) {
        return active && texture != null ? RenderType.entityTranslucent(texture) : original;
    }

    public static int bodyColor(int original) {
        return active ? (original & 0x00FFFFFF) | (GhostAlpha.alpha() << 24) : original;
    }

    public static void end() {
        active = false;
        texture = null;
    }
}
