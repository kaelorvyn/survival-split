package com.agnes.survivalsplitclient.mixin;

import com.agnes.survivalsplitclient.RenderAlphaContext;
import com.agnes.survivalsplitclient.config.GhostAlpha;
import com.agnes.survivalsplitclient.render.GhostRenderLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Environment(EnvType.CLIENT)
@Mixin(OrderedRenderCommandQueueImpl.class)
public abstract class OrderedRenderCommandQueueImplMixin {

    private static final String SUBMIT_MODEL = "submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V";
    private static final String BATCH_SUBMIT_MODEL = "Lnet/minecraft/client/render/command/BatchingRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V";

    private static final String SUBMIT_MODEL_PART = "submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;ZZILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;I)V";
    private static final String BATCH_SUBMIT_MODEL_PART = "Lnet/minecraft/client/render/command/BatchingRenderCommandQueue;submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;ZZILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;I)V";

    @ModifyArg(method = SUBMIT_MODEL,
            at = @At(value = "INVOKE", target = BATCH_SUBMIT_MODEL),
            index = 3)
    private RenderLayer survivalsplit$ghostModelLayer(RenderLayer layer) {
        return RenderAlphaContext.isActive() ? GhostRenderLayer.of(layer) : layer;
    }

    @ModifyArg(method = SUBMIT_MODEL,
            at = @At(value = "INVOKE", target = BATCH_SUBMIT_MODEL),
            index = 6)
    private int survivalsplit$ghostModelColor(int color) {
        return RenderAlphaContext.isActive() ? (color & 0x00FFFFFF) | (GhostAlpha.alpha() << 24) : color;
    }

    @ModifyArg(method = SUBMIT_MODEL_PART,
            at = @At(value = "INVOKE", target = BATCH_SUBMIT_MODEL_PART),
            index = 2)
    private RenderLayer survivalsplit$ghostPartLayer(RenderLayer layer) {
        return RenderAlphaContext.isActive() ? GhostRenderLayer.of(layer) : layer;
    }

    @ModifyArg(method = SUBMIT_MODEL_PART,
            at = @At(value = "INVOKE", target = BATCH_SUBMIT_MODEL_PART),
            index = 8)
    private int survivalsplit$ghostPartColor(int color) {
        return RenderAlphaContext.isActive() ? (color & 0x00FFFFFF) | (GhostAlpha.alpha() << 24) : color;
    }
}
