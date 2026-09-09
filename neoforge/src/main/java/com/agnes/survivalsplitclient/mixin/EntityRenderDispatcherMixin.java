package com.agnes.survivalsplitclient.mixin;

import com.agnes.survivalsplitclient.EntityIdIndex;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "extractEntity", at = @At("HEAD"))
    private <E extends Entity> void survivalsplit$indexPlayer(E entity, float partialTick,
                                                               CallbackInfoReturnable<EntityRenderState> callback) {
        if (entity instanceof AbstractClientPlayer player) {
            EntityIdIndex.put(player.getId(), player.getUUID());
        }
    }
}
