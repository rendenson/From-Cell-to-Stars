package com.celltostars.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.celltostars.client.CellRenderer;
import com.celltostars.client.ClientEvoData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cell stage: instead of the Minecraft player (Steve/skin) we draw our own cell body,
 * and nothing at all while it is still inside the egg.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void celltostars$drawCell(AbstractClientPlayer player, float yaw, float partialTick, PoseStack pose,
                                   MultiBufferSource buffers, int light, CallbackInfo ci) {
        // TODO multiplayer: other players' stage/look is not synced yet, so only the local player is a cell
        if (player != Minecraft.getInstance().player || !ClientEvoData.isCell()) return;
        ci.cancel();
        if (ClientEvoData.isHatching()) return; // still inside the egg

        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        pose.pushPose();
        CellRenderer.setupPose(pose, player.getScale(), bodyYaw);
        CellRenderer.render(ClientEvoData.get().look(), pose, buffers, light,
                LivingEntityRenderer.getOverlayCoords(player, 0.0f),
                player.tickCount + partialTick, player.level(), player.getId());
        pose.popPose();
    }
}
