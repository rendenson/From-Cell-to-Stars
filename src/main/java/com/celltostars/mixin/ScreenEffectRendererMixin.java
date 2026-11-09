package com.celltostars.mixin;

import com.celltostars.client.ClientEvoData;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cell stage: no blurry "you are under water" overlay on the screen. */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    @Inject(method = "renderWater", at = @At("HEAD"), cancellable = true, require = 0)
    private static void celltostars$noWaterOverlay(CallbackInfo ci) {
        if (ClientEvoData.isCell()) ci.cancel();
    }
}
