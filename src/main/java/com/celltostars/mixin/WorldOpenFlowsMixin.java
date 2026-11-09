package com.celltostars.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Skips the "These settings are using experimental features" screen when creating a world (caused by our soup dimension). */
@Mixin(WorldOpenFlows.class)
public abstract class WorldOpenFlowsMixin {

    @ModifyVariable(method = "confirmWorldCreation", at = @At("HEAD"), argsOnly = true, require = 0)
    private static Lifecycle celltostars$treatAsStable(Lifecycle lifecycle) {
        return Lifecycle.stable();
    }
}
