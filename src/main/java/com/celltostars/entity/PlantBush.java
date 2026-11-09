package com.celltostars.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A plant cluster, like the big plants in Spore's cell stage: a mossy core with branches,
 * and a leafy "fruit" at the tip of every branch. Cells nibble the fruits; they grow back after a while.
 */
public class PlantBush extends Entity {
    public static final int MAX_FRUITS = 6;
    private static final int REGROW_TICKS = 20 * 20;

    private static final EntityDataAccessor<Integer> FRUITS = SynchedEntityData.defineId(PlantBush.class, EntityDataSerializers.INT); // bitmask
    private static final EntityDataAccessor<Integer> BRANCHES = SynchedEntityData.defineId(PlantBush.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(PlantBush.class, EntityDataSerializers.FLOAT);

    private int regrowTimer = REGROW_TICKS;

    public PlantBush(EntityType<? extends PlantBush> type, Level level) {
        super(type, level);
    }

    public void setup(int branches, float size, float yaw) {
        this.entityData.set(BRANCHES, Mth.clamp(branches, 3, MAX_FRUITS));
        this.entityData.set(SIZE, size);
        this.entityData.set(FRUITS, (1 << branches) - 1); // all fruits there
        setYRot(yaw);
    }

    public int branches() {
        return this.entityData.get(BRANCHES);
    }

    public float size() {
        return this.entityData.get(SIZE);
    }

    public boolean hasFruit(int i) {
        return (this.entityData.get(FRUITS) & (1 << i)) != 0;
    }

    /** Where fruit number i hangs (tip of branch i). */
    public Vec3 fruitPos(int i) {
        float angle = (getYRot() + i * 360.0f / branches()) * Mth.DEG_TO_RAD;
        double r = branchLength();
        return new Vec3(getX() + Mth.cos(angle) * r, getY(), getZ() + Mth.sin(angle) * r);
    }

    public double branchLength() {
        return 0.6 * size();
    }

    /** Eats fruit i if it is there. Returns true if something was eaten. */
    public boolean eatFruit(int i) {
        if (!hasFruit(i)) return false;
        this.entityData.set(FRUITS, this.entityData.get(FRUITS) & ~(1 << i));
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FRUITS, 0);
        builder.define(BRANCHES, 5);
        builder.define(SIZE, 1.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel && --regrowTimer <= 0) {
            regrowTimer = REGROW_TICKS;
            for (int i = 0; i < branches(); i++) {
                if (!hasFruit(i)) {
                    this.entityData.set(FRUITS, this.entityData.get(FRUITS) | (1 << i));
                    break; // one fruit at a time
                }
            }
        }
    }

    /** Seen from far away (the camera is high above big cells). */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double max = 96 * Math.max(1, size());
        return distance < max * max;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    /** Helper for others: is this bush a real, living one? */
    public static boolean alive(Entity e) {
        return e instanceof PlantBush && e.isAlive();
    }
}
