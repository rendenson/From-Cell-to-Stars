package com.celltostars.logic;

import com.celltostars.CellToStars;
import com.celltostars.client.ClientEvoData;
import com.celltostars.data.Combat;
import com.celltostars.data.Diet;
import com.celltostars.data.Part;
import com.celltostars.data.EvoAttachments;
import com.celltostars.data.EvoData;
import com.celltostars.data.Stage;
import com.celltostars.entity.CellCreature;
import com.celltostars.entity.FoodParticle;
import com.celltostars.entity.PlantBush;
import com.celltostars.entity.Species;
import com.celltostars.entity.EvoEntities;
import com.celltostars.network.EvoNetwork;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import org.joml.Vector3f;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** All the game rules of the mod live here. Everything runs on the server. */
public final class EvoLogic {
    private static final double PLANE_Y = CellToStars.CELL_PLANE_Y;

    private static final double FOOD_RADIUS = 12;
    private static final int MAX_FOOD_NEAR_PLAYER = 30;
    private static final int MAX_CREATURES_NEAR_PLAYER = 10;
    private static final int MAX_BUSHES_NEAR_PLAYER = 4;
    /** Players reach level 5; levels 6 and 7 exist only as giants of the deep. */

    private static final ResourceLocation SCALE_ID = CellToStars.id("cell_scale");
    private static final ResourceLocation HEALTH_ID = CellToStars.id("cell_health");
    private static final ResourceLocation SWIM_ID = CellToStars.id("cell_flagellum");
    private static final ResourceLocation DAMAGE_ID = CellToStars.id("cell_spike");
    private static final ResourceLocation KNOCKBACK_ID = CellToStars.id("cell_knockback");
    /** Next game tick each player's proboscis may sip again. */
    private static final java.util.Map<java.util.UUID, Long> NEXT_SIP = new java.util.HashMap<>();

    private EvoLogic() {
    }

    // ---------- data access ----------

    public static EvoData get(ServerPlayer player) {
        return player.getAttachedOrCreate(EvoAttachments.SPORE);
    }

    /** Works on both sides: server reads the saved data, client reads what the server sent. */
    public static boolean isCell(Player player) {
        if (player instanceof ServerPlayer serverPlayer) return get(serverPlayer).stage() == Stage.CELL;
        return player.level().isClientSide && ClientEvoData.isCell();
    }

    /** Saves new data, applies its effects to the player and tells the client. */
    public static void set(ServerPlayer player, EvoData data) {
        EvoData old = get(player);
        player.setAttached(EvoAttachments.SPORE, data);
        applyAttributes(player, data);
        sync(player, data);
        if (data.stage() == Stage.CELL && old.stage() == Stage.CELL && data.level() > old.level()) {
            player.sendSystemMessage(Component.translatable("celltostars.msg.grew", data.level(), EvoData.MAX_LEVEL)
                    .withStyle(ChatFormatting.AQUA));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 0.8f);
        } else if (data.stage() == Stage.CELL && old.stage() == Stage.CELL && data.tier() > old.tier()) {
            // a small growth spurt between the big ones
            player.level().playSound(null, player.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.PLAYERS, 1.0f, 1.2f);
        }
    }

    public static void sync(ServerPlayer player, EvoData data) {
        ServerPlayNetworking.send(player, new EvoNetwork.SyncPayload(data));
    }

    /** Size, health, speed and damage depend on the stage, the growth level and the parts. */
    public static void applyAttributes(ServerPlayer player, EvoData data) {
        boolean cell = data.stage() == Stage.CELL;
        int tier = data.tier();
        // size: 0.3 at level 1, x1.6 every level
        setModifier(player, Attributes.SCALE, SCALE_ID,
                cell ? cellScale(tier) - 1.0 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        // health: like in Spore every cell has the same health at every size (6 hearts); +2 hearts with a membrane
        setModifier(player, Attributes.MAX_HEALTH, HEALTH_ID,
                cell ? Combat.CELL_HEALTH - 20 + (data.has(Part.MEMBRANE) ? 4 : 0) : 0,
                AttributeModifier.Operation.ADD_VALUE);
        // swimming speed is set directly on the client (see CellSpeed / CellToStarsClient), not by attributes
        setModifier(player, Attributes.WATER_MOVEMENT_EFFICIENCY, SWIM_ID, 0, AttributeModifier.Operation.ADD_VALUE);
        setModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID,
                cell ? 2 * data.count(Part.SPIKE) : 0, AttributeModifier.Operation.ADD_VALUE);
        // a bite still throws you back, but not across the whole screen (vanilla knockback is huge for a tiny cell)
        setModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID,
                cell ? 0.6 : 0, AttributeModifier.Operation.ADD_VALUE);

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        if (!cell) {
            removeCellEffects(player);
        }
    }

    /** The player's entity scale at a growth level. */
    public static double cellScale(int tier) {
        return 0.3 * EvoData.tierScale(tier);
    }

    /** "How many times bigger than at level 1" - used for spawn distances etc. */
    private static double sizeFactor(ServerPlayer player) {
        return player.getScale() / 0.3;
    }

    private static void setModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id,
                                    double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
        if (amount != 0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    // ---------- actions ----------

    /**
     * First time in a world: you start as a cell inside an egg, with the editor open to create it.
     * Re-joining: back into the egg with the editor, press "Hatch" to continue.
     * The move to the soup waits a moment - teleporting in the very tick you join breaks things.
     */
    public static void onJoin(ServerPlayer player) {
        EvoData data = get(player);
        if (!data.introDone() && data.stage() == Stage.NONE) {
            EvoScheduler.later(player, 10, p -> {
                set(p, EvoData.NEW_CELL);
                if (teleportToSoup(p)) {
                    p.setHealth(p.getMaxHealth());
                    EvoCycle.enterEgg(p, EvoNetwork.EDITOR_CREATE);
                }
            });
            return;
        }
        if (!data.introDone()) {
            data = data.withIntroDone();
            player.setAttached(EvoAttachments.SPORE, data);
        }
        applyAttributes(player, data);
        sync(player, data);
        if (data.stage() == Stage.CELL) {
            EvoScheduler.later(player, 10, p -> {
                if (p.level().dimension() != CellToStars.CELL_LEVEL && !teleportToSoup(p)) return;
                EvoCycle.enterEgg(p, EvoNetwork.EDITOR_RESUME);
            });
        }
    }

    public static void addDna(ServerPlayer player, int amount) {
        set(player, get(player).gain(amount));
    }

    /**
     * Called when a cell touches a food particle. Returns true if it was eaten.
     * Like in Spore: once the evolution bar is full, food still heals but gives no more DNA.
     */
    public static boolean tryEat(ServerPlayer player, boolean meat) {
        return tryEat(player, meat, 1);
    }

    /** @param portions how many "pieces" this food is worth (a bush fruit = 2) */
    public static boolean tryEat(ServerPlayer player, boolean meat, int portions) {
        EvoData data = get(player);
        if (data.stage() != Stage.CELL) return false;

        Diet diet = data.diet();
        int gain = (meat ? diet.meatDna : diet.plantDna) * portions;
        if (gain <= 0) {
            burp(player); // wrong mouth for this food: yuck!
            return false;
        }

        player.heal(2.0f * portions); // Spore (medium): a piece of food heals 1 of 6 HP
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.6f, 1.6f);
        EvoData eaten = data;
        for (int i = 0; i < portions; i++) eaten = eaten.ate(meat); // remember what we eat (diet path)
        if (data.canEvolve()) {
            set(player, eaten);
            return true;
        }

        EvoData updated = eaten.gain(gain);
        set(player, updated);
        if (updated.canEvolve()) {
            player.sendSystemMessage(Component.translatable("celltostars.msg.ready").withStyle(ChatFormatting.GOLD));
        }
        return true;
    }

    /** Evolving is done from the editor (inside the egg), like in Spore. The cheat command forces it. */
    public static void evolve(ServerPlayer player, boolean force) {
        EvoData data = get(player);
        if (!data.canEvolve()) return;
        if (!force && !EvoCycle.isInEgg(player)) return;
        EvoCycle.leaveEggForEvolution(player);
        // like Spore: what you ate in the cell stage decides what you are, and your creature-stage ability
        String path = data.dietPath();
        set(player, data.withStage(Stage.CREATURE).withTrait(path));
        teleportToOverworld(player);
        player.sendSystemMessage(Component.translatable("celltostars.msg.evolved").withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.translatable("celltostars.msg.path",
                Component.translatable("celltostars.path." + path),
                Component.translatable("celltostars.ability." + path)).withStyle(ChatFormatting.GOLD));
    }

    private static final java.util.Map<java.util.UUID, Long> LAST_BURP = new java.util.HashMap<>();

    /** Touching food your mouth can't eat: a burp (at most every 3 seconds). */
    private static void burp(ServerPlayer player) {
        long now = player.level().getGameTime();
        Long last = LAST_BURP.get(player.getUUID());
        if (last != null && now - last < 60) return;
        LAST_BURP.put(player.getUUID(), now);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    // ---------- teleports ----------

    public static boolean teleportToSoup(ServerPlayer player) {
        ServerLevel soup = player.server.getLevel(CellToStars.CELL_LEVEL);
        if (soup == null) {
            player.sendSystemMessage(Component.translatable("celltostars.msg.no_dimension").withStyle(ChatFormatting.RED));
            return false;
        }
        player.teleportTo(soup, 0.5, PLANE_Y, 0.5, 180.0f, 0.0f);
        return true;
    }

    public static void teleportToOverworld(ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        BlockPos top = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn);
        player.teleportTo(overworld, top.getX() + 0.5, top.getY(), top.getZ() + 0.5, player.getYRot(), 0.0f);
    }

    // ---------- world tick (only in the soup world) ----------

    public static void tickSoup(ServerLevel level) {
        long time = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            EvoData data = get(player);
            if (data.stage() != Stage.CELL) continue;

            if (time % 40 == 0) {
                // cells breathe in water, see in the dark depth and never get hungry
                keepEffect(player, MobEffects.WATER_BREATHING);
                keepEffect(player, MobEffects.NIGHT_VISION);
                // food 17 = never hungry, but no natural healing either (that needs 18+):
                // like in Spore, a cell heals only by eating
                player.getFoodData().setFoodLevel(17);
                player.getFoodData().setSaturation(0.0f);
            }
            if (EvoCycle.isInEgg(player)) continue; // in the editor: the world waits

            EvoCycle.tickMate(level, player);
            eatTouchingFood(level, player);
            if (time % 20 == 0) {
                spawnFood(level, player);
                spawnBushes(level, player);
            }
            if (time % 60 == 0) {
                spawnCreatures(level, player);
            }
            touchDamage(level, player, data); // every tick, so a short touch is never missed
        }
        keepEverythingOnPlane(level);
    }

    /** Effects are long and refreshed early, so night vision never starts blinking. */
    private static void keepEffect(ServerPlayer player, Holder<MobEffect> effect) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getDuration() < 20 * 20) {
            player.addEffect(new MobEffectInstance(effect, 20 * 40, 0, true, false, false));
        }
    }

    private static void removeCellEffects(ServerPlayer player) {
        MobEffectInstance nightVision = player.getEffect(MobEffects.NIGHT_VISION);
        if (nightVision != null && nightVision.isAmbient()) player.removeEffect(MobEffects.NIGHT_VISION);
        MobEffectInstance breathing = player.getEffect(MobEffects.WATER_BREATHING);
        if (breathing != null && breathing.isAmbient()) player.removeEffect(MobEffects.WATER_BREATHING);
    }

    /** Creatures stay on the cell plane; vanilla drops (fish items, XP) don't exist in the soup. */
    private static void keepEverythingOnPlane(ServerLevel level) {
        List<Entity> toRemove = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
                toRemove.add(entity);
            } else if (entity instanceof Mob mob && !(entity instanceof CellCreature)) { // AI cells handle their own depth
                if (Math.abs(mob.getY() - PLANE_Y) > 0.01) {
                    mob.setPos(mob.getX(), PLANE_Y, mob.getZ());
                }
                Vec3 motion = mob.getDeltaMovement();
                if (motion.y != 0) mob.setDeltaMovement(motion.x, 0, motion.z);
            }
        }
        toRemove.forEach(Entity::discard);
    }

    private static void eatTouchingFood(ServerLevel level, ServerPlayer player) {
        double k = sizeFactor(player);
        AABB mouth = player.getBoundingBox().inflate(0.15 * k, 0.6 * k, 0.15 * k);
        for (FoodParticle food : level.getEntitiesOfClass(FoodParticle.class, mouth, Entity::isAlive)) {
            if (tryEat(player, food.isMeat())) {
                food.discard();
            }
        }
        // bush fruits: the mouth is at the front of the cell
        float yawRad = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 mouthPos = player.position().add(-Mth.sin(yawRad) * 0.12 * k, 0, Mth.cos(yawRad) * 0.12 * k);
        for (PlantBush bush : level.getEntitiesOfClass(PlantBush.class, player.getBoundingBox().inflate(3 * k, 2 * k, 3 * k),
                Entity::isAlive)) {
            double reach = 0.18 * k + 0.2 * bush.size();
            for (int i = 0; i < bush.branches(); i++) {
                if (!bush.hasFruit(i)) continue;
                Vec3 fruit = bush.fruitPos(i);
                double dx = fruit.x - mouthPos.x;
                double dz = fruit.z - mouthPos.z;
                if (dx * dx + dz * dz < reach * reach && tryEat(player, false, 1)) {
                    bush.eatFruit(i);
                }
            }
        }
    }

    private static void spawnFood(ServerLevel level, ServerPlayer player) {
        double radius = FOOD_RADIUS * sizeFactor(player);
        AABB area = player.getBoundingBox().inflate(radius + 4, 2, radius + 4);
        int count = level.getEntitiesOfClass(FoodParticle.class, area).size();
        RandomSource random = level.getRandom();
        for (int i = count; i < MAX_FOOD_NEAR_PLAYER && i < count + 4; i++) {
            double x = player.getX() + (random.nextDouble() * 2 - 1) * radius;
            double z = player.getZ() + (random.nextDouble() * 2 - 1) * radius;
            spawnFoodAt(level, x, z, random.nextFloat() < 0.2f, (float) sizeFactor(player));
        }
    }

    /** Keeps a few plant bushes around each player (the big plants of Spore's cell stage). */
    private static void spawnBushes(ServerLevel level, ServerPlayer player) {
        double k = sizeFactor(player);
        AABB area = player.getBoundingBox().inflate(24 * k, 4 * k, 24 * k);
        List<PlantBush> bushes = level.getEntitiesOfClass(PlantBush.class, area.inflate(30 * k, 0, 30 * k));
        // bushes made for a much smaller cell, or left far behind, disappear
        for (PlantBush bush : bushes) {
            if (bush.size() < k * 0.45 || bush.distanceTo(player) > 40 * k) bush.discard();
        }
        if (level.getEntitiesOfClass(PlantBush.class, area).size() >= MAX_BUSHES_NEAR_PLAYER) return;

        RandomSource random = level.getRandom();
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = (8 + random.nextDouble() * 12) * k;
        double x = player.getX() + Math.cos(angle) * distance;
        double z = player.getZ() + Math.sin(angle) * distance;
        PlantBush bush = EvoEntities.PLANT_BUSH.create(level);
        if (bush == null) return;
        bush.setup(4 + random.nextInt(3), (float) (k * (0.8 + random.nextDouble() * 0.5)), random.nextFloat() * 360f);
        bush.moveTo(x, PLANE_Y, z, bush.getYRot(), 0);
        level.addFreshEntity(bush);
    }

    public static void spawnFoodAt(ServerLevel level, double x, double z, boolean meat) {
        spawnFoodAt(level, x, z, meat, 1.0f);
    }

    /** @param size 1 = food for a level-1 cell; food grows with the player so it stays visible */
    public static void spawnFoodAt(ServerLevel level, double x, double z, boolean meat, float size) {
        double y = PLANE_Y + 0.1;
        if (!level.getBlockState(BlockPos.containing(x, y, z)).is(Blocks.WATER)) return;
        FoodParticle food = EvoEntities.FOOD_PARTICLE.create(level);
        if (food == null) return;
        food.setMeat(meat);
        food.setSize(size);
        food.moveTo(x, y, z, 0, 0);
        level.addFreshEntity(food);
    }

    /**
     * Keeps a handful of AI cells around each player. Most are about the player's size,
     * some are a level smaller (food for meat eaters!) and some a level bigger (danger).
     */
    /** Test command: puts a creature right in front of the player. */
    public static void spawnCreatureInFront(ServerPlayer player, Species species, int tier) {
        if (!(player.level() instanceof ServerLevel level)) return;
        float yawRad = player.getYRot() * Mth.DEG_TO_RAD;
        double d = 1.5 * sizeFactor(player);
        CellCreature cell = EvoEntities.CELL_CREATURE.create(level);
        if (cell == null) return;
        cell.setup(species, tier);
        cell.moveTo(player.getX() - Mth.sin(yawRad) * d, PLANE_Y, player.getZ() + Mth.cos(yawRad) * d,
                player.getYRot() + 180f, 0);
        level.addFreshEntity(cell);
    }

    /** Test command: a ring of food around the player. */
    public static void spawnFoodBurst(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        double k = sizeFactor(player);
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI * 2 / 16;
            spawnFoodAt(level, player.getX() + Math.cos(a) * 1.5 * k, player.getZ() + Math.sin(a) * 1.5 * k, i % 4 == 0, (float) k);
        }
    }

    /**
     * Keeps a handful of AI cells around each player, picked like Spore does it: every species has a fixed
     * size, so which ones you meet depends on how big YOU are. Mostly peers and prey, some predators,
     * now and then an epic one; giants that you will meet later swim in the deep background.
     */
    private static void spawnCreatures(ServerLevel level, ServerPlayer player) {
        double k = sizeFactor(player);
        AABB area = player.getBoundingBox().inflate(22 * k, 10 * k, 22 * k);
        List<CellCreature> near = level.getEntitiesOfClass(CellCreature.class, area);
        if (near.size() >= MAX_CREATURES_NEAR_PLAYER) return;

        RandomSource random = level.getRandom();
        int playerTier = get(player).tier();
        // now and then one of your own kind (friendly), otherwise a species that fits your size
        boolean kin = random.nextFloat() < 0.08f && near.stream().filter(c -> c.species() == Species.KIN).count() < 2;
        Species species = kin ? Species.KIN : pickSpecies(random, playerTier, random.nextFloat() < 0.12f, near);
        if (species == null) return;
        int cellTier = kin ? playerTier : species.tier;
        boolean giant = cellTier - playerTier >= 3;

        double angle = random.nextDouble() * Math.PI * 2;
        double distance = (giant ? 14 + random.nextDouble() * 10 : 7 + random.nextDouble() * 5) * k; // outside the screen
        double x = player.getX() + Math.cos(angle) * distance;
        double z = player.getZ() + Math.sin(angle) * distance;
        if (!level.getBlockState(BlockPos.containing(x, PLANE_Y, z)).is(Blocks.WATER)) return;

        CellCreature cell = EvoEntities.CELL_CREATURE.create(level);
        if (cell == null) return;
        cell.setup(species, cellTier);
        cell.moveTo(x, giant ? PLANE_Y - 7 * k : PLANE_Y, z, random.nextFloat() * 360f, 0);
        level.addFreshEntity(cell);
    }

    /**
     * A species for the player's size. {@code giant}: one of the big ones for the deep background.
     * Otherwise: prey (1 tier smaller), peers (most), predators (1 bigger) and rarely one epic (2 bigger) at a time.
     */
    private static Species pickSpecies(RandomSource random, int playerTier, boolean giant, List<CellCreature> near) {
        boolean epicAround = near.stream().anyMatch(c -> c.cellTier() - playerTier == 2);
        float total = 0;
        float[] weights = new float[Species.values().length];
        for (Species s : Species.values()) {
            if (s.spawnWeight <= 0) continue;
            int gap = s.tier - playerTier;
            float w;
            if (giant) {
                w = gap == 3 || gap == 4 ? 1 : 0;
            } else if (playerTier < s.appearsAt || gap < -1 || gap > 2) {
                w = 0;
            } else {
                w = switch (gap) {
                    case -1 -> 1.0f;                     // prey
                    case 0 -> 1.6f;                      // peers
                    case 1 -> 0.7f;                      // predators
                    default -> epicAround ? 0 : 0.25f;   // epic
                };
            }
            weights[s.ordinal()] = w * s.spawnWeight;
            total += weights[s.ordinal()];
        }
        if (total <= 0) return null;
        float roll = random.nextFloat() * total;
        for (Species s : Species.values()) {
            roll -= weights[s.ordinal()];
            if (roll < 0 && weights[s.ordinal()] > 0) return s;
        }
        return null;
    }

    /**
     * Omnivore proboscis, like in Spore: it latches onto a living cell and drinks its juice -
     * the victim loses health, you heal and get 1 DNA (counted as meat on the diet path).
     */
    private static void siphon(ServerLevel level, ServerPlayer player, LivingEntity target) {
        EvoData data = get(player);
        Combat.Relation victim = relationOf(target, data.tier());
        // one quick grab = one sip of half a proboscis "hit"; then you bounce apart and it can't be grabbed for 3 s.
        // So a cell your size needs 4 grabs, a small one 2 (your heal and the hold are the proboscis' advantage)
        float sip = Combat.proboscisShare(victim) * 0.5f;
        if (target instanceof CellCreature cell && !cell.latch(player)) return;
        if (!target.hurt(level.damageSources().playerAttack(player), target.getMaxHealth() * sip)) return;

        player.heal(1.5f);
        juice(level, target, player);

        EvoData updated = data.ate(true);
        if (!data.canEvolve()) {
            updated = updated.gain(1);
            if (updated.canEvolve()) {
                player.sendSystemMessage(Component.translatable("celltostars.msg.ready").withStyle(ChatFormatting.GOLD));
            }
        }
        set(player, updated);
    }

    /** The "slurp": drink sound and red drops flying from the victim into the drinker. */
    public static void juice(ServerLevel level, LivingEntity from, LivingEntity to) {
        level.playSound(null, to.blockPosition(), SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 0.9f, 1.2f);
        double k = to.getScale() / 0.3;
        DustParticleOptions drops = new DustParticleOptions(new Vector3f(0.8f, 0.1f, 0.15f), (float) Math.min(4.0, 0.5 * k));
        for (int i = 0; i < 6; i++) {
            double t = i / 6.0;
            level.sendParticles(drops,
                    from.getX() + (to.getX() - from.getX()) * t, to.getY() + 0.15 * k,
                    from.getZ() + (to.getZ() - from.getZ()) * t, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Your front (mouth / spikes) hits what it touches - never your sides or back. How much a hit takes
     * follows Spore's table (see Combat): it depends on the part and on how big the victim is compared to you.
     */
    private static void touchDamage(ServerLevel level, ServerPlayer player, EvoData data) {
        boolean swallows = data.diet() == Diet.CARNIVORE;   // jaws swallow small cells whole
        boolean sips = data.diet() == Diet.OMNIVORE;        // the proboscis sucks juice from living cells
        long now = level.getGameTime();
        boolean sipNow = sips && now >= NEXT_SIP.getOrDefault(player.getUUID(), 0L); // one sip per second
        int spikes = data.count(Part.SPIKE);
        // where the tip of the part is on the model (per unit of scale): proboscis tip, spike tips, jaws
        double reach = sips ? 0.85 : spikes > 0 ? 0.72 : 0.6;
        Vec3 front = Combat.frontPoint(player, reach);
        double thickness = sips ? 0.28 : 0.38; // jaws and spikes cover the whole front, the tube is thinner

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(player.getBbWidth() * 2),
                e -> e != player && e.isAlive() && !(e instanceof Player) && Combat.reaches(player, reach, thickness, e))) {
            if (target instanceof CellCreature cell && cell.species() == Species.KIN) continue; // your own kind
            Combat.Relation victim = relationOf(target, data.tier());
            if (victim == Combat.Relation.EPIC) continue; // far too big to even scratch

            if (sips && sipNow) {
                NEXT_SIP.put(player.getUUID(), now + 20);
                sipNow = false;
                siphon(level, player, target);
            }
            if (swallows && victim == Combat.Relation.PREY) {
                // smaller than you: swallowed whole (leaves meat that you eat right away)
                target.hurt(level.damageSources().playerAttack(player), Float.MAX_VALUE);
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.6f, 1.4f);
                continue;
            }
            float share = Combat.playerHitShare(data.diet(), spikes, victim);
            if (share > 0 && target.hurt(level.damageSources().playerAttack(player), target.getMaxHealth() * share)) {
                // a clear "chomp" so you know the bite landed
                level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8f, 1.3f);
                level.sendParticles(ParticleTypes.CRIT, front.x, front.y, front.z, 6, 0.05, 0.02, 0.05, 0.05);
            }
        }
    }

    /** How a creature relates to the player by size (PREDATOR = the creature is a level bigger). */
    private static Combat.Relation relationOf(LivingEntity target, int playerTier) {
        int targetTier = target instanceof CellCreature cell ? cell.cellTier() : playerTier;
        return Combat.Relation.of(targetTier, playerTier);
    }

    /** A creature killed by a cell leaves meat behind. */
    public static void onCreatureKilled(ServerLevel level, LivingEntity dead) {
        float size = dead.getScale() / 0.3f * 0.7f;
        for (int i = 0; i < 2; i++) {
            spawnFoodAt(level,
                    dead.getX() + (level.getRandom().nextDouble() - 0.5) * 0.6 * size,
                    dead.getZ() + (level.getRandom().nextDouble() - 0.5) * 0.6 * size,
                    true, size);
        }
    }
}
