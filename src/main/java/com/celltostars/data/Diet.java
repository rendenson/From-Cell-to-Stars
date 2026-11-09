package com.celltostars.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum Diet implements StringRepresentable {
    // like in Spore: every piece of food you can eat gives exactly 1 DNA
    HERBIVORE("herbivore", 1, 0),
    CARNIVORE("carnivore", 0, 1),
    OMNIVORE("omnivore", 1, 0);  // plants from the ground; meat only by sucking juice from living cells (proboscis)

    public static final Codec<Diet> CODEC = StringRepresentable.fromEnum(Diet::values);

    private final String name;
    /** DNA gained from a plant particle. 0 = cannot eat it. */
    public final int plantDna;
    /** DNA gained from a meat particle. 0 = cannot eat it. */
    public final int meatDna;

    Diet(String name, int plantDna, int meatDna) {
        this.name = name;
        this.plantDna = plantDna;
        this.meatDna = meatDna;
    }

    public Diet next() {
        Diet[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public String translationKey() {
        return "celltostars.diet." + name;
    }

    public static Diet byName(String name) {
        for (Diet d : values()) {
            if (d.name.equals(name)) return d;
        }
        return null;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
