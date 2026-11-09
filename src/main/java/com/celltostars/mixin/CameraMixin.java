package com.celltostars.mixin;

import com.celltostars.client.ClientEvoData;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cell stage: camera looks straight down from above, like in Spore. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private boolean detached;

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    /** 3 blocks above a level-1 cell (higher for bigger cells); during the love scene the camera slowly dives in close. */
    private static double cameraHeight(Entity cell) {
        double k = cell instanceof net.minecraft.world.entity.LivingEntity living ? living.getScale() / 0.3 : 1.0;
        long loveStart = ClientEvoData.loveStartMs();
        if (loveStart == 0) return 3.0 * k;
        double t = Math.min(1.0, (Util.getMillis() - loveStart) / 2000.0);
        return (3.0 - 2.2 * t * t) * k;
    }

    @Inject(method = "setup", at = @At("TAIL"))
    private void celltostars$topDownCamera(BlockGetter level, Entity entity, boolean detachedArg, boolean thirdPersonReverse,
                                        float partialTick, CallbackInfo ci) {
        if (entity != Minecraft.getInstance().player || !ClientEvoData.isCell()) return;

        Vec3 pos = entity.getPosition(partialTick);
        this.detached = true;
        // yaw 180 = north is always "up" on the screen; the camera stays under water, just above the cell
        setRotation(180.0f, 90.0f);
        setPosition(pos.x, pos.y + cameraHeight(entity), pos.z);
    }
}
