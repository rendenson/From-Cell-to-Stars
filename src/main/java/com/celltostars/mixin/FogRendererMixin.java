package com.celltostars.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.celltostars.client.ClientEvoData;
import com.celltostars.client.SoupColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cell stage: clear water right around the cell, then thick murk, so the soup feels bottomless.
 * The murk has its own colour for every growth level (see SoupColors).
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Shadow
    private static float fogRed;
    @Shadow
    private static float fogGreen;
    @Shadow
    private static float fogBlue;

    @Inject(method = "setupFog", at = @At("TAIL"), require = 0)
    private static void celltostars$murkyDepth(CallbackInfo ci) {
        if (!ClientEvoData.isCell()) return;
        var player = Minecraft.getInstance().player;
        float k = player != null ? player.getScale() / 0.3f : 1.0f; // bigger cell = camera higher = see further
        RenderSystem.setShaderFogStart(2.0f * k);
        RenderSystem.setShaderFogEnd(14.0f * k);
    }

    @Inject(method = "setupColor", at = @At("TAIL"), require = 0)
    private static void celltostars$layerColour(CallbackInfo ci) {
        if (!ClientEvoData.isCell()) return;
        float[] c = SoupColors.current(ClientEvoData.get().level());
        fogRed = c[0];
        fogGreen = c[1];
        fogBlue = c[2];
        RenderSystem.clearColor(c[0], c[1], c[2], 0.0f);
    }
}
