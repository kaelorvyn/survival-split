package net.minecraft.client.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderLayer.class)
public interface RenderLayerAccessor {

    @Accessor("renderSetup")
    RenderSetup renderSetup();

    @Invoker("of")
    static RenderLayer of(String name, RenderSetup renderSetup) {
        throw new AssertionError();
    }
}
