package com.celltostars.mixin;

import com.celltostars.client.ClientEvoData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cell stage (client only): the mouse doesn't turn the cell, and there is no sprint. */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void celltostars$noMouseTurn(CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer && ClientEvoData.isCell()) {
            ci.cancel();
        }
    }

    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void celltostars$noSprint(boolean sprinting, CallbackInfo ci) {
        if (sprinting && (Object) this instanceof LocalPlayer && ClientEvoData.isCell()) {
            ci.cancel();
        }
    }
}
