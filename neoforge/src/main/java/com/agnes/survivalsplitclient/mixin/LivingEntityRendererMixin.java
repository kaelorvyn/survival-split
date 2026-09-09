package com.agnes.survivalsplitclient.mixin;

import com.agnes.survivalsplitclient.RenderAlphaContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "submit", at = @At("HEAD"))
    private void survivalsplit$begin(LivingEntityRenderState state, PoseStack poseStack,
                                     SubmitNodeCollector collector, CameraRenderState camera,
                                     CallbackInfo callback) {
        if (state instanceof AvatarRenderState playerState) {
            RenderAlphaContext.begin(playerState.id, playerState.skin.body().texturePath());
        }
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void survivalsplit$end(LivingEntityRenderState state, PoseStack poseStack,
                                   SubmitNodeCollector collector, CameraRenderState camera,
                                   CallbackInfo callback) {
        RenderAlphaContext.end();
    }

    @ModifyArg(
            method = "submit",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            index = 3
    )
    private RenderType survivalsplit$translucentBody(RenderType original) {
        return RenderAlphaContext.bodyRenderType(original);
    }

    @ModifyArg(
            method = "submit",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            index = 6
    )
    private int survivalsplit$translucentColor(int original) {
        return RenderAlphaContext.bodyColor(original);
    }
}
