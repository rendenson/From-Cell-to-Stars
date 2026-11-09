package com.celltostars.mixin;

import com.celltostars.client.ClientEvoData;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cell stage: hide all of the vanilla HUD (hotbar, hearts, hunger, air, XP, crosshair). Only the Spore HUD stays. */
@Mixin(Gui.class)
public abstract class GuiMixin {

    @Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true, require = 0)
    private void celltostars$hideHotbar(CallbackInfo ci) {
        if (ClientEvoData.isCell()) ci.cancel();
    }

    /** Vanilla hearts, hunger, armor and air bubbles - the cell has its own health bar instead. */
    @Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true, require = 0)
    private void celltostars$hideVanillaStatus(CallbackInfo ci) {
        if (ClientEvoData.isCell()) ci.cancel();
    }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true, require = 0)
    private void celltostars$hideXpBar(CallbackInfo ci) {
        if (ClientEvoData.isCell()) ci.cancel();
    }

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true, require = 0)
    private void celltostars$hideXpLevel(CallbackInfo ci) {
        if (ClientEvoData.isCell()) ci.cancel();
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void celltostars$hideCrosshair(CallbackInfo ci) {
        if (ClientEvoData.isCell()) ci.cancel();
    }
}
