package com.agnes.survivalsplitclient.render;

import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.RenderSetupAccessor;
import net.minecraft.client.render.TextureSpecAccessor;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.function.Supplier;

public final class RenderSetupTextures {

    private RenderSetupTextures() {
    }

    public static Identifier firstTexture(RenderSetup setup) {
        Object spec = firstTextureSpec(setup);
        return spec == null ? null : ((TextureSpecAccessor) spec).location();
    }

    public static Supplier<GpuSampler> firstSampler(RenderSetup setup) {
        Object spec = firstTextureSpec(setup);
        return spec == null ? null : ((TextureSpecAccessor) spec).sampler();
    }

    private static Object firstTextureSpec(RenderSetup setup) {
        RenderSetupAccessor accessor = (RenderSetupAccessor) (Object) setup;
        Map<String, ?> textures = accessor.textures();
        return textures.values().stream().findFirst().orElse(null);
    }
}
