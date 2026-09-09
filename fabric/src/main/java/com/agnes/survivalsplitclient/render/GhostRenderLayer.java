package com.agnes.survivalsplitclient.render;

import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.RenderLayerAccessor;
import net.minecraft.client.render.RenderSetupAccessor;

public final class GhostRenderLayer {

    private GhostRenderLayer() {
    }

    public static RenderLayer of(RenderLayer original) {
        if (original == null || original.isOutline()) {
            return original;
        }
        RenderLayerAccessor layerAccessor = (RenderLayerAccessor) original;
        RenderSetup setup = layerAccessor.renderSetup();
        RenderSetupAccessor setupAccessor = (RenderSetupAccessor) (Object) setup;
        var texture = RenderSetupTextures.firstTexture(setup);
        if (texture == null) {
            return original;
        }

        RenderSetup.Builder builder = RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT)
                .translucent()
                .outlineMode(setupAccessor.outlineMode());
        var sampler = RenderSetupTextures.firstSampler(setup);
        if (sampler != null) {
            builder.texture("Sampler0", texture, sampler);
        } else {
            builder.texture("Sampler0", texture);
        }
        if (setupAccessor.useLightmap()) {
            builder.useLightmap();
        }
        if (setupAccessor.useOverlay()) {
            builder.useOverlay();
        }
        if (setupAccessor.hasCrumbling()) {
            builder.crumbling();
        }
        if (setupAccessor.layeringTransform() != null) {
            builder.layeringTransform(setupAccessor.layeringTransform());
        }
        if (setupAccessor.outputTarget() != null) {
            builder.outputTarget(setupAccessor.outputTarget());
        }
        if (setupAccessor.textureTransform() != null) {
            builder.textureTransform(setupAccessor.textureTransform());
        }
        builder.expectedBufferSize(setupAccessor.expectedBufferSize());

        return RenderLayerAccessor.of("survivalsplit_ghost", builder.build());
    }
}
