package net.minecraft.client.render;

import net.minecraft.client.gl.GpuSampler;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Supplier;

@Mixin(RenderSetup.TextureSpec.class)
public interface TextureSpecAccessor {

    @Accessor("location")
    Identifier location();

    @Accessor("sampler")
    Supplier<GpuSampler> sampler();
}
