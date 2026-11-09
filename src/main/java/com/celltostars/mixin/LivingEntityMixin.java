package com.celltostars.mixin;

import com.celltostars.client.ClientEvoData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cell stage (client only): no sprinting. LivingEntity.setSprinting also adds a +30% speed bonus,
 * so it has to be stopped here and not only in Entity (that was the "Ctrl makes me faster" bug).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void celltostars$noSprintBonus(boolean sprinting, CallbackInfo ci) {
        if (sprinting && (Object) this instanceof LocalPlayer && ClientEvoData.isCell()) {
            ci.cancel();
        }
    }
}
