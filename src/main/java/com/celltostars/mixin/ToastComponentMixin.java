package com.celltostars.mixin;

import com.celltostars.client.ClientEvoData;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cell stage: no "New Recipes Unlocked!" and similar pop-ups - a cell doesn't craft. */
@Mixin(ToastComponent.class)
public abstract class ToastComponentMixin {

    @Inject(method = "addToast", at = @At("HEAD"), cancellable = true, require = 0)
    private void celltostars$noToastsForCells(CallbackInfo ci) {
        if (ClientEvoData.isCell()) ci.cancel();
    }
}
