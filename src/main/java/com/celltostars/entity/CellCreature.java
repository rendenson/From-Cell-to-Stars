package com.celltostars.entity;

import com.celltostars.CellToStars;
import com.celltostars.data.CellSpeed;
import com.celltostars.data.Combat;
import com.celltostars.data.Diet;
import com.celltostars.data.Part;
import com.celltostars.data.EvoData;
import com.celltostars.logic.EvoCycle;
import com.celltostars.logic.EvoLogic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * An AI cell in the soup. The species decides how it looks, behaves and how big it is - like in Spore,
 * a species never grows; the player grows past it.
 *
 * Compared to the player's size tier (half levels):
 *  - 2+ tiers smaller: too tiny to matter -> disappears
 *  - 1 smaller (prey) / same (peer) / 1 bigger (predator) / 2 bigger (epic): on the player's plane
 *  - 3+ tiers bigger: a giant swimming deep below in the murk, out of reach; it rises to the plane
 *    once the player has grown close to its size
 *
 * Speed does NOT grow with size (like Spore): it is measured in "screen sizes" of the player's view.
 */
public class CellCreature extends PathfinderMob {
    private static final EntityDataAccessor<Integer> SPECIES = SynchedEntityData.defineId(CellCreature.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(CellCreature.class, EntityDataSerializers.INT);

    /** How far in front of the body the mouth / spike tips reach (times the entity scale). */
    private static final double FRONT_REACH = 0.62;

    private Vec3 wanderDir = Vec3.ZERO;
    private int wanderTimer = 0;
    private int scaredTimer = 0;
    private int angryTimer = 0;
    private int fleeTicks = 0;       // keeps running for a moment once spooked (no back-and-forth)
    /** AI cells swim at this share of a player cell's speed with the same number of flagella. */
    private static final double AI_SPEED = 0.8;      // omnivores fight back for a while after being attacked
    private int attackCooldown = 0;
    private int stunTicks = 0;       // just got hit: flung back, can't steer
    private int chaseTicks = 0;      // hunters give up a chase after a while...
    private int restTicks = 0;       // ...and rest before the next one
    private ServerPlayer latchedTo;  // a proboscis is sucking on me
    private int latchTicks = 0;
    private int latchCooldown = 0;   // just broke free: can't be grabbed again for a moment
    /** How long one proboscis grab lasts: a quick stab-and-sip, then both bounce apart. */
    public static final int LATCH_TICKS = 16;

    public CellCreature(EntityType<? extends CellCreature> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, Combat.CELL_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SPECIES, 0);
        builder.define(TIER, 2);
    }

    public Species species() {
        return Species.byIndex(this.entityData.get(SPECIES));
    }

    /** Its size tier (2 = level 1.0 ... 12 = level 6.0). */
    public int cellTier() {
        return this.entityData.get(TIER);
    }

    /** Called right after creating it: the species decides the size. All cells have the same health. */
    public void setup(Species species) {
        setup(species, species.tier);
    }

    /** With an explicit size (your own kind is always your size; test command). */
    public void setup(Species species, int tier) {
        this.entityData.set(SPECIES, species.ordinal());
        this.entityData.set(TIER, tier);
        AttributeInstance scaleAttr = getAttribute(Attributes.SCALE);
        if (scaleAttr != null) scaleAttr.setBaseValue(0.3 * EvoData.tierScale(tier));
        setHealth(getMaxHealth());
    }

    /**
     * A proboscis wants to grab me. Returns true if it may sip now: either it just grabbed me, or it is
     * already holding me. After a grab ends I tear free and can't be grabbed again for 3 seconds.
     */
    public boolean latch(ServerPlayer player) {
        if (latchTicks > 0) return latchedTo == player;
        if (latchCooldown > 0) return false;
        this.latchedTo = player;
        this.latchTicks = LATCH_TICKS;
        return true;
    }

    private int bounceCooldown = 0;

    /**
     * Two jaws met head-on: a "clack", I'm pushed back and so is the other cell. Returns false if we just
     * bounced (so it isn't repeated every tick).
     */
    public boolean bounceOff(LivingEntity other) {
        if (bounceCooldown > 0 || !(level() instanceof ServerLevel server)) return false;
        bounceCooldown = 10;
        flingFrom(other, 0.25);
        stunTicks = 6;
        other.knockback(0.6, getX() - other.getX(), getZ() - other.getZ());
        other.hurtMarked = true;
        server.playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.7f, 1.6f);
        Vec3 mid = position().add(other.position()).scale(0.5);
        server.sendParticles(ParticleTypes.CRIT, mid.x, getY() + 0.1 * getScale() / 0.3, mid.z, 5, 0.05, 0.02, 0.05, 0.1);
        return true;
    }

    /** Pushes me away from the given point (a flinch) and stuns me for a moment. */
    private void flingFrom(Entity from, double strength) {
        Vec3 away = new Vec3(getX() - from.getX(), 0, getZ() - from.getZ());
        if (away.lengthSqr() > 1.0e-6) {
            Vec3 push = away.normalize().scale(strength * getScale() / 0.3);
            setDeltaMovement(push.x, 0, push.z);
            stunTicks = 8;
        }
    }

    /** The client moves me to my new spot right away instead of sliding there over 3 ticks (no "ghost" lag). */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        super.lerpTo(x, y, z, yRot, xRot, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (latchCooldown > 0) latchCooldown--;
        if (bounceCooldown > 0) bounceCooldown--;
    }

    @Override
    protected void registerGoals() {
        // no vanilla goals: see think()
    }

    @Override
    public boolean shouldBeSaved() {
        return false; // new cells keep spawning around the player anyway
    }

    @Override
    public void checkDespawn() {
        // we remove cells ourselves in think()
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() != null) {
            if (source.getEntity() instanceof Player) {
                if (species().temperament == Species.Temperament.NEUTRAL) angryTimer = 120; // fights back
                else scaredTimer = 60;                                                   // runs away for 3 seconds
            }
            // flinch: a short jerk away from whoever hit me
            Vec3 away = new Vec3(getX() - source.getEntity().getX(), 0, getZ() - source.getEntity().getZ());
            if (away.lengthSqr() > 1.0e-6) {
                Vec3 push = away.normalize().scale(0.25 * getScale() / 0.3);
                setDeltaMovement(push.x, 0, push.z);
                stunTicks = 6;
            }
        }
        return hurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server && isAlive()) {
            think(server);
        }
    }

    // ---------------- AI ----------------

    private void think(ServerLevel level) {
        if (attackCooldown > 0) attackCooldown--;
        if (scaredTimer > 0) scaredTimer--;
        if (angryTimer > 0) angryTimer--;
        if (restTicks > 0) restTicks--;

        Species species = species();
        double myK = EvoData.tierScale(cellTier()); // my size compared to a level-1 cell

        ServerPlayer player = nearestCellPlayer(level, 80 * myK);
        if (player == null) {
            if (tickCount % 40 == 0) discard();
            return;
        }
        EvoData playerData = EvoLogic.get(player);
        if (species == Species.KIN && cellTier() != playerData.tier()) {
            setup(Species.KIN, playerData.tier()); // your own kind grows along with you
        }
        int gap = cellTier() - playerData.tier(); // + = I am bigger
        if (gap <= -2) { // the player outgrew me: I'm too tiny to matter any more
            discard();
            return;
        }
        boolean background = gap >= 3;
        double playerK = player.getScale() / 0.3; // the "screen size": speeds are measured in it
        if (distanceTo(player) > (background ? 45 : 32) * Math.max(myK, playerK)) { // left far behind: gone, a new one spawns ahead
            discard();
            return;
        }

        // giants live deep below the player's plane, and rise up once the player is close to their size
        double targetY = background ? CellToStars.CELL_PLANE_Y - 7 * playerK : CellToStars.CELL_PLANE_Y;
        double dy = targetY - getY();
        if (Math.abs(dy) > 0.01) setPos(getX(), getY() + Mth.clamp(dy, -0.2 * playerK, 0.2 * playerK), getZ());

        // stuck on a proboscis: held at the tip of the tube, can't swim away
        if (latchTicks > 0 && latchedTo != null && latchedTo.isAlive()) {
            latchTicks--;
            Vec3 tip = Combat.frontPoint(latchedTo, 0.85);
            float yaw = latchedTo.getYRot() * Mth.DEG_TO_RAD;
            double hold = getBbWidth() * 0.5;
            setPos(tip.x - Mth.sin(yaw) * hold, getY(), tip.z + Mth.cos(yaw) * hold);
            setDeltaMovement(Vec3.ZERO);
            if (latchTicks == 0) { // tear free and get away; the drinker bounces back too
                flingFrom(latchedTo, 0.35);
                latchedTo.knockback(0.5, getX() - latchedTo.getX(), getZ() - latchedTo.getZ());
                latchedTo.hurtMarked = true; // tell the player's client about the push
                latchCooldown = 60;
                scaredTimer = 60;
                latchedTo = null;
            }
            return;
        }
        if (stunTicks > 0) { // flung back by a hit: just drift
            stunTicks--;
            return;
        }

        // same speed rule as the player, measured in the player's screen size (NOT our own size)
        // AI cells swim a bit slower than a player cell with the same parts: with a couple of flagella you can
        // catch the cells of your layer, without any you can only catch the slow ones (like in Spore)
        double speed = CellSpeed.blocksPerTick(species.speedPoints, playerK) * AI_SPEED;
        if (background) {
            wander(speed * 0.4); // giants just cruise slowly, they don't care about something so small
            return;
        }

        double playerDist = distanceTo(player);
        double sense = playerK; // one "cell length" of yours on screen, roughly
        // is the player swimming towards me? (cells react to something coming at them, not to everything they see)
        Vec3 playerMove = new Vec3(player.getX() - player.xo, 0, player.getZ() - player.zo);
        Vec3 toMe = new Vec3(getX() - player.getX(), 0, getZ() - player.getZ());
        boolean playerComing = playerMove.lengthSqr() > 1.0e-6 && playerMove.dot(toMe) > 0.5 * playerMove.length() * toMe.length();
        if (fleeTicks > 0) fleeTicks--;
        Combat.Relation meToPlayer = Combat.Relation.of(cellTier(), playerData.tier());
        // can the player hurt me? (meat eaters, sucking tubes and spikes do; harmless herbivores don't)
        boolean playerDangerous = playerData.diet() != Diet.HERBIVORE || playerData.has(Part.SPIKE);
        boolean playerBigger = meToPlayer == Combat.Relation.PREY;
        boolean iAmBig = meToPlayer == Combat.Relation.PREDATOR || meToPlayer == Combat.Relation.EPIC;
        boolean eatsPlants = species.look.diet() != Diet.CARNIVORE;

        Vec3 goal = null;      // swim towards this
        Vec3 danger = null;    // or away from this
        double goalSpeed = 0.85;

        switch (species.temperament) {
            case PASSIVE -> {
                // calm grazers: run only when hurt, or when something dangerous comes right at them
                if (scaredTimer > 0 || (playerDangerous && !iAmBig && playerComing && playerDist < 1.3 * sense)) fleeTicks = 30;
                if (fleeTicks > 0) danger = player.position();
                else goal = nearestFood(level, false, 4 * playerK);
                goalSpeed = 0.6;
            }
            case SKITTISH -> {
                // nervous: anything coming closer makes them dart away
                if (scaredTimer > 0 || (!iAmBig && playerDist < 1.8 * sense && (playerComing || playerDist < sense))) fleeTicks = 30;
                if (fleeTicks > 0) danger = player.position();
                else goal = nearestFood(level, false, 4 * playerK);
                goalSpeed = 0.6;
            }
            case HUNTER -> {
                boolean weak = getHealth() < getMaxHealth() * 0.3f;
                boolean afraid = weak || scaredTimer > 0 || (playerDangerous && playerBigger);
                if (afraid && playerDist < 2.0 * sense) {
                    danger = player.position();
                    chaseTicks = 0;
                } else if (!playerBigger && restTicks == 0 && playerDist < 4 * sense) {
                    goal = chase(player.position(), 100, 160); // gives up after ~5 s (Spore: you can get away)
                    goalSpeed = 0.8;
                } else {
                    CellCreature prey = nearestPrey(level, 6 * playerK);
                    goal = prey != null ? prey.position() : nearestFood(level, true, 4 * playerK);
                }
            }
            case DRIFTER -> {
                // big and lazy: turns towards you slowly and lets you swim into its mouth
                if (!playerBigger && restTicks == 0 && playerDist < 3 * sense) {
                    goal = chase(player.position(), 120, 200);
                    goalSpeed = 0.55;
                } else if (playerBigger && playerDangerous && playerDist < 2 * sense) {
                    danger = player.position();
                    goalSpeed = 0.6;
                }
            }
            case NEUTRAL -> {
                if (angryTimer > 0 && !playerBigger) {
                    goal = player.position(); // you started it
                } else if (scaredTimer > 0 || (playerDangerous && playerBigger && playerComing && playerDist < 1.3 * sense)) {
                    danger = player.position();
                } else {
                    goal = nearestFood(level, false, 4 * playerK);
                    goalSpeed = 0.6;
                }
            }
            case KIN -> {
                goal = playerDist > 12 * sense ? player.position() : nearestFood(level, !eatsPlants, 3 * playerK);
                goalSpeed = 0.5;
            }
        }

        double cruise = species.temperament == Species.Temperament.DRIFTER ? 0.3 : 0.45;
        if (danger != null) {
            swim(position().subtract(danger), speed);              // running away: full speed
        } else if (goal != null) {
            swim(goal.subtract(position()), speed * goalSpeed);    // chasing / going for food
        } else {
            wander(speed * cruise);                                // just drifting around
        }

        touch(level, species, playerData);
    }

    /** Chasing something, but only for {@code maxTicks}; then a rest of {@code restFor} ticks. */
    private Vec3 chase(Vec3 target, int maxTicks, int restFor) {
        if (++chaseTicks > maxTicks) {
            chaseTicks = 0;
            restTicks = restFor;
            return null;
        }
        return target;
    }

    private void wander(double speed) {
        if (--wanderTimer <= 0 || wanderDir == Vec3.ZERO) {
            double angle = random.nextDouble() * Math.PI * 2;
            wanderDir = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            wanderTimer = 60 + random.nextInt(80);
        }
        swim(wanderDir, speed);
    }

    /** Turns smoothly towards the direction and swims forward (along where it faces). */
    private void swim(Vec3 dir, double speed) {
        if (dir.horizontalDistanceSqr() < 1.0e-6) return;
        float targetYaw = (float) (Mth.atan2(-dir.x, dir.z) * Mth.RAD_TO_DEG);
        float turn = species().temperament == Species.Temperament.DRIFTER ? 5.0f : 12.0f; // big ones turn slowly
        float yaw = Mth.approachDegrees(getYRot(), targetYaw, turn);
        setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        float rad = yaw * Mth.DEG_TO_RAD;
        setDeltaMovement(-Mth.sin(rad) * speed, 0, Mth.cos(rad) * speed);
    }

    /** Biting / stinging with the FRONT of the body (mouth / spikes), and eating food it touches. */
    private void touch(ServerLevel level, Species species, EvoData nearestData) {
        Vec3 front = Combat.frontPoint(this, FRONT_REACH * species.bodyLength);
        double radius = 0.08 * getScale();

        boolean wantsToBite = species.bites() && switch (species.temperament) {
            case HUNTER -> scaredTimer == 0;
            case DRIFTER -> true;
            case NEUTRAL -> angryTimer > 0;
            default -> false;
        };
        if (attackCooldown == 0 && (wantsToBite || species.hasSpikes())) {
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(getBbWidth()),
                    p -> EvoLogic.isCell(p) && !EvoCycle.isInEgg(p) && Combat.touches(front, p, radius))) {
                EvoData victim = EvoLogic.get(player);
                Combat.Relation rel = Combat.Relation.of(cellTier(), victim.tier());
                // my jaws against your jaws, face to face: they clack and we bounce apart (only spikes still sting)
                boolean jawsMeet = species.look.diet() == Diet.CARNIVORE && victim.diet() == Diet.CARNIVORE
                        && rel != Combat.Relation.EPIC && Combat.headOn(this, player);
                if (jawsMeet) bounceOff(player);
                float share = 0;
                if (wantsToBite && !jawsMeet && rel != Combat.Relation.PREY) share = Combat.aiHitShare(rel); // jaws don't bother with giants
                if (species.hasSpikes()) share = Math.max(share, Combat.aiHitShare(rel));        // spikes always sting
                if (share > 0 && player.hurt(level.damageSources().mobAttack(this), player.getMaxHealth() * share)) {
                    if (species.look.diet() == Diet.OMNIVORE && wantsToBite) {
                        EvoLogic.juice(level, player, this); // its proboscis drinks from you
                    }
                    if (wantsToBite) heal(2.0f); // a bite is a meal
                    attackCooldown = 20;
                }
            }
        }
        if (attackCooldown == 0 && wantsToBite) {
            // predators eat smaller cells whole and bite peers
            for (CellCreature prey : level.getEntitiesOfClass(CellCreature.class, getBoundingBox().inflate(getBbWidth()),
                    c -> c != this && c.isAlive() && c.cellTier() <= cellTier() && Combat.touches(front, c, radius))) {
                if (species.look.diet() == Diet.CARNIVORE && prey.species().look.diet() == Diet.CARNIVORE
                        && prey.cellTier() == cellTier() && Combat.headOn(this, prey)) {
                    prey.bounceOff(this); // two equal jaws: clack, nobody gets bitten
                    attackCooldown = 20;
                    continue;
                }
                boolean peer = prey.cellTier() == cellTier();
                if (prey.hurt(level.damageSources().mobAttack(this), prey.getMaxHealth() * (peer ? 0.5f : 1.0f))) {
                    heal(prey.isAlive() ? 2.0f : 4.0f); // swallowing a whole cell fills it up more
                }
                attackCooldown = 20;
            }
        }

        AABB body = getBoundingBox().inflate(0.05, 0.6, 0.05);
        boolean eatsMeat = species.look.diet() == Diet.CARNIVORE; // a proboscis can't eat dead meat (Spore)
        boolean eatsPlants = species.look.diet() != Diet.CARNIVORE;
        // plant eaters nibble bush fruits too, but slowly (so they don't strip a bush before you get there)
        if (eatsPlants && tickCount % 60 == 0) {
            for (PlantBush bush : level.getEntitiesOfClass(PlantBush.class, body.inflate(1.0 * getScale() / 0.3), Entity::isAlive)) {
                for (int i = 0; i < bush.branches(); i++) {
                    if (bush.hasFruit(i) && bush.fruitPos(i).distanceToSqr(position().x, bush.getY(), position().z)
                            < Math.pow(getBbWidth() + 0.15 * bush.size(), 2) && bush.eatFruit(i)) {
                        heal(2.0f);
                        break;
                    }
                }
            }
        }
        for (FoodParticle food : level.getEntitiesOfClass(FoodParticle.class, body, Entity::isAlive)) {
            if (food.isMeat() ? eatsMeat : eatsPlants) {
                food.discard();
                heal(2.0f); // like the player: a piece of food heals 1 of Spore's 6 HP
            }
        }
    }

    private ServerPlayer nearestCellPlayer(ServerLevel level, double range) {
        Player p = level.getNearestPlayer(getX(), getY(), getZ(), range,
                e -> e instanceof ServerPlayer sp && EvoLogic.isCell(sp) && !sp.isSpectator());
        return p instanceof ServerPlayer sp ? sp : null;
    }

    private Vec3 nearestFood(ServerLevel level, boolean meat, double range) {
        List<FoodParticle> foods = level.getEntitiesOfClass(FoodParticle.class, getBoundingBox().inflate(range, 1, range),
                f -> f.isAlive() && f.isMeat() == meat);
        return foods.stream().min(Comparator.comparingDouble(this::distanceToSqr)).map(Entity::position).orElse(null);
    }

    private CellCreature nearestPrey(ServerLevel level, double range) {
        return level.getEntitiesOfClass(CellCreature.class, getBoundingBox().inflate(range, 1, range),
                        c -> c != this && c.isAlive() && c.species() != Species.KIN && c.cellTier() < cellTier())
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Species", this.entityData.get(SPECIES));
        tag.putInt("CellTier", this.entityData.get(TIER));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(SPECIES, tag.getInt("Species"));
        this.entityData.set(TIER, Math.max(2, tag.getInt("CellTier")));
    }
}
