package com.celltostars.mixin;

import com.celltostars.client.ClientEvoData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Minecraft hears the world from the camera. In the cell stage the camera hangs high above the cell
 * (up to ~20 blocks for a big cell), which is out of hearing range for most sounds.
 * So the "ears" are moved down into the cell itself.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    @Redirect(method = "updateSource",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getPosition()Lnet/minecraft/world/phys/Vec3;"),
            require = 0)
    private Vec3 celltostars$earsInTheCell(Camera camera) {
        Player player = Minecraft.getInstance().player;
        if (player != null && ClientEvoData.isCell()) {
            return player.position().add(0, player.getBbHeight() * 0.5, 0);
        }
        return camera.getPosition();
    }
}
