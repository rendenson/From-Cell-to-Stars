package com.celltostars.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the mod remembers about one player. Immutable: every change makes a new copy.
 *
 * @param dna       spendable points (spent in the editor)
 * @param progress  evolution bar, 0..EVOLVE_AT
 * @param introDone true once the player has been turned into a cell for the first time in this world
 * @param name      the name the player gave their creature
 * @param primary   main body colour (0xRRGGBB)
 * @param secondary pattern colour (0xRRGGBB)
 * @param pattern   pattern id, see {@link Pattern}
 * @param plantEaten how many plant pieces were eaten in the cell stage (decides the diet path)
 * @param meatEaten  how many meat pieces were eaten
 * @param trait      what the cell stage made you: "herbivore" / "omnivore" / "carnivore" ("" = not decided yet)
 */
public record EvoData(Stage stage, int dna, int progress, Diet diet, List<String> parts, boolean introDone, String name,
                        int primary, int secondary, String pattern,
                        int plantEaten, int meatEaten, String trait) {
    public static final int DEFAULT_PRIMARY = 0x6FC3DF;
    public static final int DEFAULT_SECONDARY = 0x2E6E8E;
    public static final int MAX_LEVEL = 5;

    public static final int EVOLVE_AT = 150; // 30 per growth level

    public static final EvoData EMPTY = new EvoData(Stage.NONE, 0, 0, Diet.HERBIVORE, List.of(), false, "", DEFAULT_PRIMARY, DEFAULT_SECONDARY, "spots", 0, 0, "");
    public static final EvoData NEW_CELL = new EvoData(Stage.CELL, 0, 0, Diet.HERBIVORE, List.of(), true, "", DEFAULT_PRIMARY, DEFAULT_SECONDARY, "spots", 0, 0, "");

    public static final Codec<EvoData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Stage.CODEC.optionalFieldOf("stage", Stage.NONE).forGetter(EvoData::stage),
            Codec.INT.optionalFieldOf("dna", 0).forGetter(EvoData::dna),
            Codec.INT.optionalFieldOf("progress", 0).forGetter(EvoData::progress),
            Diet.CODEC.optionalFieldOf("diet", Diet.HERBIVORE).forGetter(EvoData::diet),
            Codec.STRING.listOf().optionalFieldOf("parts", List.of()).forGetter(EvoData::parts),
            Codec.BOOL.optionalFieldOf("intro_done", false).forGetter(EvoData::introDone),
            Codec.STRING.optionalFieldOf("name", "").forGetter(EvoData::name),
            Codec.INT.optionalFieldOf("primary", DEFAULT_PRIMARY).forGetter(EvoData::primary),
            Codec.INT.optionalFieldOf("secondary", DEFAULT_SECONDARY).forGetter(EvoData::secondary),
            Codec.STRING.optionalFieldOf("pattern", "spots").forGetter(EvoData::pattern),
            Codec.INT.optionalFieldOf("plant_eaten", 0).forGetter(EvoData::plantEaten),
            Codec.INT.optionalFieldOf("meat_eaten", 0).forGetter(EvoData::meatEaten),
            Codec.STRING.optionalFieldOf("trait", "").forGetter(EvoData::trait)
    ).apply(i, EvoData::new));

    public static final StreamCodec<ByteBuf, EvoData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public EvoData {
        parts = List.copyOf(parts);
    }

    public boolean has(Part part) {
        return parts.contains(part.id());
    }

    /** How many of this part the cell has (flagella and spikes can stack). */
    public int count(Part part) {
        int n = 0;
        for (String p : parts) {
            if (p.equals(part.id())) n++;
        }
        return n;
    }

    public boolean canEvolve() {
        return stage == Stage.CELL && progress >= EVOLVE_AT;
    }

    public EvoData withStage(Stage newStage) {
        return new EvoData(newStage, dna, progress, diet, parts, introDone, name, primary, secondary, pattern, plantEaten, meatEaten, trait);
    }

    public EvoData withDiet(Diet newDiet) {
        return new EvoData(stage, dna, progress, newDiet, parts, introDone, name, primary, secondary, pattern, plantEaten, meatEaten, trait);
    }

    public EvoData withIntroDone() {
        return new EvoData(stage, dna, progress, diet, parts, true, name, primary, secondary, pattern, plantEaten, meatEaten, trait);
    }

    /** Adds DNA and the same amount of evolution progress. */
    public EvoData gain(int amount) {
        return new EvoData(stage, Math.max(0, dna + amount),
                Math.min(EVOLVE_AT, Math.max(0, progress + amount)), diet, parts, introDone, name, primary, secondary, pattern, plantEaten, meatEaten, trait);
    }

    public EvoData withLook(int newPrimary, int newSecondary, String newPattern) {
        return new EvoData(stage, dna, progress, diet, parts, introDone, name, newPrimary, newSecondary, newPattern, plantEaten, meatEaten, trait);
    }

    /** Growth level 1..5: the cell grows as the evolution bar fills (every 20 points). */
    public int level() {
        return 1 + Math.min(MAX_LEVEL - 1, progress / (EVOLVE_AT / MAX_LEVEL));
    }

    /**
     * Size "tier" = half growth levels, like Spore's ~10 small growth spurts: tier 2 = level 1.0,
     * tier 3 = level 1.5 ... tier 11 = level 5.5. Every 15 points of the bar the cell grows a bit.
     */
    public int tier() {
        return 2 + Math.min(2 * MAX_LEVEL - 1, progress / (EVOLVE_AT / (2 * MAX_LEVEL)));
    }

    /** How big a cell of this level is compared to level 1: x1.6 every level (level 5 = 6.5x). */
    public static float levelScale(int level) {
        return (float) Math.pow(1.6, level - 1);
    }

    /** Same for tiers (tier 2 = 1x, every tier x1.26). */
    public static float tierScale(int tier) {
        return (float) Math.pow(1.6, (tier - 2) / 2.0);
    }

    /**
     * How much stronger (health, damage) a cell of this level is compared to level 1.
     * Grows much slower than the size, so a giant is tough but not a one-hit monster for its peers.
     */
    public static float levelPower(int level) {
        return 1.0f + 0.5f * (level - 1);
    }

    /** Jumps the growth bar to the start of a level (test command). */
    public EvoData atLevel(int level) {
        int p = Math.min(EVOLVE_AT, (level - 1) * (EVOLVE_AT / MAX_LEVEL));
        return new EvoData(stage, dna, p, diet, parts, introDone, name, primary, secondary, pattern, plantEaten, meatEaten, trait);
    }

    public CellLook look() {
        return new CellLook(primary, secondary, Pattern.byId(pattern), diet,
                count(Part.FLAGELLUM), count(Part.SPIKE), has(Part.MEMBRANE));
    }

    /** Counts what was eaten, for the herbivore/omnivore/carnivore path. */
    public EvoData ate(boolean meat) {
        return new EvoData(stage, dna, progress, diet, parts, introDone, name, primary, secondary, pattern,
                plantEaten + (meat ? 0 : 1), meatEaten + (meat ? 1 : 0), trait);
    }

    /** 0 = only plants, 1 = only meat, 0.5 = half/half (or nothing eaten yet). */
    public float meatShare() {
        int total = plantEaten + meatEaten;
        return total == 0 ? 0.5f : meatEaten / (float) total;
    }

    /** What the eating so far makes you, like Spore's end-of-stage result. */
    public String dietPath() {
        float meat = meatShare();
        if (plantEaten + meatEaten == 0) return "omnivore";
        return meat < 0.25f ? "herbivore" : meat > 0.75f ? "carnivore" : "omnivore";
    }

    public EvoData withTrait(String newTrait) {
        return new EvoData(stage, dna, progress, diet, parts, introDone, name, primary, secondary, pattern,
                plantEaten, meatEaten, newTrait);
    }

    public EvoData withName(String newName) {
        return new EvoData(stage, dna, progress, diet, parts, introDone, newName, primary, secondary, pattern, plantEaten, meatEaten, trait);
    }

    /** Removes one copy of a part and gives its DNA back (like selling a part in the Spore editor). */
    public EvoData sell(Part part) {
        List<String> newParts = new ArrayList<>(parts);
        newParts.remove(part.id());
        return new EvoData(stage, dna + part.cost(), progress, diet, newParts, introDone, name, primary, secondary, pattern, plantEaten, meatEaten, trait);
    }

    public EvoData buy(Part part) {
        List<String> newParts = new ArrayList<>(parts);
        newParts.add(part.id());
        return new EvoData(stage, dna - part.cost(), progress, diet, newParts, introDone, name, primary, secondary, pattern, plantEaten, meatEaten, trait);
    }
}
