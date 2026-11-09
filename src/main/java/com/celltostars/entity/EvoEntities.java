package com.celltostars.entity;

import com.celltostars.CellToStars;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class EvoEntities {
    public static final EntityType<FoodParticle> FOOD_PARTICLE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CellToStars.id("food_particle"),
            EntityType.Builder.<FoodParticle>of(FoodParticle::new, MobCategory.MISC)
                    .sized(0.35f, 0.35f)
                    .clientTrackingRange(6)
                    .updateInterval(4)
                    .build("food_particle"));

    public static final EntityType<MateCell> MATE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CellToStars.id("mate"),
            EntityType.Builder.<MateCell>of(MateCell::new, MobCategory.MISC)
                    .sized(0.2f, 0.2f)
                    .clientTrackingRange(8)
                    .updateInterval(4)
                    .build("mate"));

    public static final EntityType<HatchShell> HATCH_SHELL = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CellToStars.id("hatch_shell"),
            EntityType.Builder.<HatchShell>of(HatchShell::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f)
                    .clientTrackingRange(8)
                    .updateInterval(10)
                    .build("hatch_shell"));

    public static final EntityType<PlantBush> PLANT_BUSH = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CellToStars.id("plant_bush"),
            EntityType.Builder.<PlantBush>of(PlantBush::new, MobCategory.MISC)
                    .sized(0.4f, 0.3f)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build("plant_bush"));

    public static final EntityType<CellCreature> CELL_CREATURE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CellToStars.id("cell_creature"),
            EntityType.Builder.<CellCreature>of(CellCreature::new, MobCategory.WATER_AMBIENT)
                    .sized(0.6f, 0.6f)
                    .clientTrackingRange(8)
                    .updateInterval(1) // position every tick: small fast cells are where you see them
                    .build("cell_creature"));

    private EvoEntities() {
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(CELL_CREATURE, CellCreature.createAttributes());
    }
}
