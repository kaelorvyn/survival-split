package net.minecraft.client.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(RenderSetup.class)
public interface RenderSetupAccessor {

    @Accessor("textures")
    Map<String, RenderSetup.TextureSpec> textures();

    @Accessor("useLightmap")
    boolean useLightmap();

    @Accessor("useOverlay")
    boolean useOverlay();

    @Accessor("hasCrumbling")
    boolean hasCrumbling();

    @Accessor("outlineMode")
    RenderSetup.OutlineMode outlineMode();

    @Accessor("layeringTransform")
    LayeringTransform layeringTransform();

    @Accessor("outputTarget")
    OutputTarget outputTarget();

    @Accessor("textureTransform")
    TextureTransform textureTransform();

    @Accessor("expectedBufferSize")
    int expectedBufferSize();
}
