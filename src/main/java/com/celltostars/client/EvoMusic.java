package com.celltostars.client;

import com.celltostars.EvoSounds;
import net.minecraft.sounds.Music;

/**
 * Music settings for the cell stage. Kept out of the Minecraft mixin on purpose: fields there would be
 * created when the game starts, before our sounds are registered.
 */
public final class EvoMusic {
    private static Music editor;
    private static Music soup;

    private EvoMusic() {
    }

    /** Starts right away and repeats. */
    public static Music editor() {
        if (editor == null) editor = new Music(EvoSounds.MUSIC_EDITOR, 0, 0, true);
        return editor;
    }

    /** With quiet gaps of 10-45 seconds between plays. */
    public static Music soup() {
        if (soup == null) soup = new Music(EvoSounds.MUSIC_SOUP, 200, 900, true);
        return soup;
    }
}
