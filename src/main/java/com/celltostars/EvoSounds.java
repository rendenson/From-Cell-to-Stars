package com.celltostars;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/** Our own music (files in assets/celltostars/sounds/music, listed in sounds.json). */
public final class EvoSounds {
    public static final Holder.Reference<SoundEvent> MUSIC_EDITOR = register("music.editor");
    public static final Holder.Reference<SoundEvent> MUSIC_SOUP = register("music.soup");

    private EvoSounds() {
    }

    private static Holder.Reference<SoundEvent> register(String name) {
        ResourceLocation id = CellToStars.id(name);
        return Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void init() {
    }
}
