package com.celltostars.mixin;

import com.celltostars.client.EvoMusic;
import com.celltostars.client.ClientEvoData;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.Music;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Picks the background music: our calm editor track while inside the egg, our watery ambient track
 * while swimming as a cell. Vanilla's MusicManager then plays, switches and repeats them for us.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMusicMixin {
    @Inject(method = "getSituationalMusic", at = @At("HEAD"), cancellable = true, require = 0)
    private void celltostars$cellMusic(CallbackInfoReturnable<Music> cir) {
        if (!ClientEvoData.isCell()) return;
        cir.setReturnValue(ClientEvoData.isInEgg() ? EvoMusic.editor() : EvoMusic.soup());
    }
}
