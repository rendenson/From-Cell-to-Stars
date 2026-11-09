package com.celltostars.entity;

import com.celltostars.data.CellLook;
import com.celltostars.data.Diet;
import com.celltostars.data.Pattern;

/**
 * The AI cells of the soup, built the way Spore's cell stage is: every species has a FIXED size and
 * does not grow. You grow past them: a species first shows up as a predator (or an epic giant) when you
 * are small, becomes your peer, then your prey, and finally is too tiny to see.
 *
 * Sizes are in "tiers" = half growth levels: tier 2 = level 1.0, tier 3 = level 1.5 ... tier 11 = level 5.5.
 * The roster mirrors the structure of Spore's 27 cells (size, diet, speed, temperament), with our own cells.
 */
public enum Species {
    // ---- level 1 ----
    BUBBLER("bubbler", look(0x9FE3C9, 0x4FA889, Pattern.SPOTS, Diet.HERBIVORE, 1, 0), 2, 2, 0, Temperament.PASSIVE, 1.0f, 1.0f, 6),
    NIPPER("nipper", look(0xF2B33D, 0x8A5A12, Pattern.STRIPES, Diet.CARNIVORE, 2, 0), 3, 3, 3, Temperament.HUNTER, 0.9f, 1.1f, 2),
    PRICKLE("prickle", look(0xC9D46A, 0x6E7A20, Pattern.PATCHES, Diet.HERBIVORE, 1, 1), 3, 3, 1, Temperament.PASSIVE, 1.1f, 0.9f, 4),
    SNAPJAW("snapjaw", look(0xD9483B, 0x5E1A14, Pattern.STRIPES, Diet.CARNIVORE, 2, 0), 4, 3, 2, Temperament.HUNTER, 1.0f, 1.0f, 3),
    // ---- level 2 ----
    GAPER("gaper", look(0x7FB2E6, 0x2F5E9E, Pattern.NONE, Diet.HERBIVORE, 3, 0), 4, 4, 3, Temperament.SKITTISH, 1.2f, 0.85f, 4),
    THORNGAPER("thorngaper", look(0x5C86C9, 0xE6E0B0, Pattern.SPOTS, Diet.CARNIVORE, 3, 1), 4, 4, 3, Temperament.HUNTER, 1.2f, 0.85f, 2),
    JETTER("jetter", look(0xE07A2E, 0xFFD9A0, Pattern.STRIPES, Diet.CARNIVORE, 4, 0), 5, 4, 1, Temperament.HUNTER, 0.85f, 1.25f, 2),
    GRUB("grub", look(0xB98AD9, 0x6A3E8C, Pattern.SPOTS, Diet.HERBIVORE, 1, 0), 5, 5, 1, Temperament.PASSIVE, 1.0f, 1.3f, 4),
    SQUIRTER("squirter", look(0x86C94E, 0x2E5E14, Pattern.PATCHES, Diet.CARNIVORE, 2, 0), 6, 5, 2, Temperament.HUNTER, 1.1f, 1.0f, 2),
    LANCER("lancer", look(0x8C2F2F, 0xF2C94C, Pattern.STRIPES, Diet.CARNIVORE, 1, 2), 7, 5, 1, Temperament.DRIFTER, 0.8f, 1.5f, 1),
    // ---- level 3 ----
    FUZZY("fuzzy", look(0xF29BC2, 0xFFFFFF, Pattern.SPOTS, Diet.HERBIVORE, 4, 0), 6, 6, 1, Temperament.PASSIVE, 1.05f, 1.0f, 4),
    PUNK("punk", look(0x3D3D52, 0xFF4FA3, Pattern.STRIPES, Diet.CARNIVORE, 5, 0), 6, 6, 3, Temperament.HUNTER, 0.95f, 1.05f, 2),
    SIPPER("sipper", look(0xE8D44D, 0x7A5C1E, Pattern.PATCHES, Diet.OMNIVORE, 5, 0), 7, 6, 4, Temperament.NEUTRAL, 0.9f, 1.2f, 3),
    GULPER("gulper", look(0x4A8FA8, 0xF2A33D, Pattern.SPOTS, Diet.CARNIVORE, 1, 0), 8, 6, 1, Temperament.DRIFTER, 1.4f, 1.0f, 1),
    URCHIN("urchin", look(0x6A3E8C, 0xD9A3F2, Pattern.PATCHES, Diet.HERBIVORE, 2, 3), 7, 7, 1, Temperament.PASSIVE, 1.1f, 1.1f, 3),
    // ---- level 4 ----
    TRISPIKE("trispike", look(0x9AD94E, 0x3E5E14, Pattern.STRIPES, Diet.HERBIVORE, 2, 2), 9, 7, 2, Temperament.PASSIVE, 1.0f, 1.2f, 2),
    JAWLORD("jawlord", look(0x5E1A14, 0xD9483B, Pattern.PATCHES, Diet.CARNIVORE, 1, 2), 9, 7, 1, Temperament.DRIFTER, 1.25f, 1.25f, 1),
    PUFFER("puffer", look(0xE6C24E, 0x8C5A1E, Pattern.SPOTS, Diet.CARNIVORE, 0, 3), 10, 8, 0, Temperament.DRIFTER, 1.3f, 1.0f, 1),
    GLIMMER("glimmer", look(0x4EE6E0, 0x1E5E8C, Pattern.SPOTS, Diet.HERBIVORE, 1, 0), 8, 8, 0, Temperament.PASSIVE, 1.0f, 1.0f, 4),
    NEEDLER("needler", look(0x1E5E8C, 0x4EE6E0, Pattern.STRIPES, Diet.CARNIVORE, 2, 1), 9, 9, 1, Temperament.HUNTER, 0.9f, 1.3f, 3),
    // ---- level 5 ----
    MAW("maw", look(0x8C1E4E, 0xF2D35B, Pattern.PATCHES, Diet.CARNIVORE, 1, 3), 11, 9, 0, Temperament.DRIFTER, 1.4f, 1.1f, 1),
    ZIPPER("zipper", look(0xF2F2F2, 0x3DA5E6, Pattern.STRIPES, Diet.HERBIVORE, 5, 0), 10, 10, 4, Temperament.SKITTISH, 0.8f, 1.3f, 3),
    HATCHLING("hatchling", look(0xF2C2A3, 0xC9584E, Pattern.SPOTS, Diet.CARNIVORE, 2, 0), 10, 10, 1, Temperament.HUNTER, 0.9f, 0.9f, 4),
    MATRON("matron", look(0x9ADBA3, 0xF2C2A3, Pattern.PATCHES, Diet.HERBIVORE, 1, 0), 11, 10, 0, Temperament.PASSIVE, 1.2f, 1.2f, 2),
    PATRON("patron", look(0xC9584E, 0x9ADBA3, Pattern.PATCHES, Diet.CARNIVORE, 3, 0), 11, 10, 2, Temperament.HUNTER, 1.1f, 1.2f, 2),
    SNOUT("snout", look(0x2E2E3D, 0xE65C3D, Pattern.STRIPES, Diet.CARNIVORE, 5, 0), 12, 10, 4, Temperament.HUNTER, 0.9f, 1.4f, 1),
    BLOB("blob", look(0xC2A3F2, 0x5C3D8C, Pattern.SPOTS, Diet.OMNIVORE, 4, 0), 12, 11, 3, Temperament.NEUTRAL, 1.35f, 1.1f, 2),

    /** Your own species: drawn with your look, friendly, always your size. Spawned separately. */
    KIN("kin", look(0xFFFFFF, 0xFFFFFF, Pattern.NONE, Diet.HERBIVORE, 1, 0), 2, 99, 1, Temperament.KIN, 1.0f, 1.0f, 0);

    /** How a species behaves (Spore's "medium" difficulty). */
    public enum Temperament {
        PASSIVE,   // herbivore: grazes, runs from things that can hurt it
        SKITTISH,  // runs from anything that comes close
        HUNTER,    // carnivore: chases cells smaller or equal to it (gives up after a while)
        DRIFTER,   // big predator: drifts slowly and bites whatever swims into its mouth
        NEUTRAL,   // omnivore: minds its own business, but fights back when attacked
        KIN        // your own kind
    }

    public final String id;
    public final CellLook look;
    /** Its fixed size (tier = 2 x growth level) - it's your peer when you are at this tier. */
    public final int tier;
    /** It doesn't show up before you reach this tier (Spore's "appears at level"). */
    public final int appearsAt;
    /** Speed points like the player's flagella (0..4) - Spore's speed 1..5. */
    public final int speedPoints;
    public final Temperament temperament;
    /** Body shape: width and length multipliers (just looks). */
    public final float bodyWidth;
    public final float bodyLength;
    /** How common it is among the species that fit (0 = spawned separately). */
    public final int spawnWeight;

    Species(String id, CellLook look, int tier, int appearsAt, int speedPoints, Temperament temperament,
            float bodyWidth, float bodyLength, int spawnWeight) {
        this.id = id;
        this.look = look;
        this.tier = tier;
        this.appearsAt = appearsAt;
        this.speedPoints = speedPoints;
        this.temperament = temperament;
        this.bodyWidth = bodyWidth;
        this.bodyLength = bodyLength;
        this.spawnWeight = spawnWeight;
    }

    private static CellLook look(int primary, int secondary, Pattern pattern, Diet diet, int flagella, int spikes) {
        return new CellLook(primary, secondary, pattern, diet, flagella, spikes, false);
    }

    /** Jaws bite (carnivores); a proboscis sips (omnivores). */
    public boolean bites() {
        return look.diet() != Diet.HERBIVORE;
    }

    public boolean hasSpikes() {
        return look.spikes() > 0;
    }

    public static Species byId(String id) {
        for (Species s : values()) {
            if (s.id.equals(id)) return s;
        }
        return null;
    }

    public static Species byIndex(int i) {
        Species[] all = values();
        return all[Math.floorMod(i, all.length)];
    }
}
