package com.celltostars.mixin;

import com.celltostars.client.ClientEvoData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cell stage controls, like Spore: W/A/S/D move up/left/down/right on the screen
 * (the camera always looks north), and the cell turns towards where it swims.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void celltostars$screenRelativeMovement(CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !ClientEvoData.isCell()) return;

        Input input = (Input) (Object) this;
        input.jumping = false;
        input.shiftKeyDown = false;

        // screen right = east (+X), screen down = south (+Z)
        // frozen while in the egg, during the love scene and while the egg cracks open
        boolean frozen = ClientEvoData.isInEgg() || ClientEvoData.loveStartMs() != 0 || ClientEvoData.isHatching();
        float dx = frozen ? 0 : (input.right ? 1 : 0) - (input.left ? 1 : 0);
        float dz = frozen ? 0 : (input.down ? 1 : 0) - (input.up ? 1 : 0);
        // vanilla water movement is switched off: the speed is set in CellToStarsClient.applySwimSpeed
        input.forwardImpulse = 0;
        input.leftImpulse = 0;
        ClientEvoData.setSwimming(dx != 0 || dz != 0);
        if (dx == 0 && dz == 0) return;

        // Minecraft yaw: 0 = south, 90 = west, 180 = north, -90 = east
        float targetYaw = (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);
        float yaw = Mth.approachDegrees(player.getYRot(), targetYaw, 25.0f); // turn up to 25 degrees per tick
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
        player.setXRot(0);
    }
}
