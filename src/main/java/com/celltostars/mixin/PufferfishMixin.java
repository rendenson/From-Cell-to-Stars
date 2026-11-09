package com.celltostars.mixin;

import com.celltostars.logic.EvoLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla pufferfish sting anything within ~1 block - for a tiny cell that is miles away.
 * Cells get our own contact-only sting instead (EvoLogic.pufferfishSting).
 */
@Mixin(Pufferfish.class)
public abstract class PufferfishMixin {

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void celltostars$noLongRangeSting(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer && EvoLogic.isCell(serverPlayer)) {
            ci.cancel();
        }
    }
}
