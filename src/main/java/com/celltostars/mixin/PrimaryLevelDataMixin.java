package com.celltostars.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Our soup world is a datapack dimension, which Minecraft marks as "experimental" and then shows
 * the "Worlds using Experimental Settings are not supported" screen every time. Skip that warning.
 */
@Mixin(PrimaryLevelData.class)
public abstract class PrimaryLevelDataMixin {

    @Inject(method = "worldGenSettingsLifecycle", at = @At("HEAD"), cancellable = true, require = 0)
    private void celltostars$noExperimentalWarning(CallbackInfoReturnable<Lifecycle> cir) {
        cir.setReturnValue(Lifecycle.stable());
    }
}
