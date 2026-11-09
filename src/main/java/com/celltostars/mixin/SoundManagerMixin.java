package com.celltostars.mixin;

import com.celltostars.client.ClientEvoData;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While you are in the editor (inside the egg) the soup is silent: only music and menu clicks are heard. */
@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void celltostars$quietEgg(SoundInstance sound, CallbackInfo ci) {
        if (!ClientEvoData.isInEgg()) return;
        SoundSource source = sound.getSource();
        if (source != SoundSource.MUSIC && source != SoundSource.MASTER) {
            ci.cancel();
        }
    }
}
