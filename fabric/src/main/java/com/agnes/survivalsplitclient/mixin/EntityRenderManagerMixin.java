package com.agnes.survivalsplitclient.mixin;

import com.agnes.survivalsplitclient.EntityIdIndex;
import com.agnes.survivalsplitclient.RenderAlphaContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(EntityRenderManager.class)
public abstract class EntityRenderManagerMixin {

    @Inject(method = "getAndUpdateRenderState(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/entity/state/EntityRenderState;",
            at = @At("HEAD"))
    private void survivalsplit$indexEntity(Entity entity, float tickDelta,
                                           CallbackInfoReturnable<EntityRenderState> cir) {
        if (entity instanceof PlayerEntity) {
            EntityIdIndex.put(entity.getId(), entity.getUuid());
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/render/state/CameraRenderState;DDDLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V",
            at = @At("HEAD"))
    private void survivalsplit$beginAlpha(EntityRenderState state, CameraRenderState camera, double x, double y,
                                          double z, MatrixStack matrices, OrderedRenderCommandQueue queue,
                                          CallbackInfo ci) {
        if (state instanceof PlayerEntityRenderState playerState) {
            RenderAlphaContext.begin(playerState.id);
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/render/state/CameraRenderState;DDDLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V",
            at = @At("RETURN"))
    private void survivalsplit$endAlpha(EntityRenderState state, CameraRenderState camera, double x, double y,
                                        double z, MatrixStack matrices, OrderedRenderCommandQueue queue,
                                        CallbackInfo ci) {
        RenderAlphaContext.end();
    }
}
