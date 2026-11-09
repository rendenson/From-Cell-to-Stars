package com.celltostars.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** A floating piece of food in the primordial soup. Plant (green kelp) or meat (raw beef). */
public class FoodParticle extends Entity implements ItemSupplier {
    private static final EntityDataAccessor<Boolean> MEAT = SynchedEntityData.defineId(FoodParticle.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(FoodParticle.class, EntityDataSerializers.FLOAT);
    private static final int LIFETIME = 20 * 120;

    private ItemStack plantStack;
    private ItemStack meatStack;

    public FoodParticle(EntityType<? extends FoodParticle> type, Level level) {
        super(type, level);
    }

    public boolean isMeat() {
        return this.entityData.get(MEAT);
    }

    public void setMeat(boolean meat) {
        this.entityData.set(MEAT, meat);
    }

    /** 1 = food for a level-1 cell; bigger for bigger cells. */
    public float getSize() {
        return this.entityData.get(SIZE);
    }

    public void setSize(float size) {
        this.entityData.set(SIZE, size);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MEAT, false);
        builder.define(SIZE, 1.0f);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            if (tickCount > LIFETIME) {
                discard();
                return;
            }
            // slow horizontal drift, changes direction now and then (never up/down: cells live on one plane)
            if (tickCount % 40 == 0) {
                setDeltaMovement(
                        (random.nextDouble() - 0.5) * 0.02 * getSize(),
                        0,
                        (random.nextDouble() - 0.5) * 0.02 * getSize());
            }
        }

        move(MoverType.SELF, getDeltaMovement());
    }

    @Override
    public ItemStack getItem() {
        if (isMeat()) {
            if (meatStack == null) meatStack = new ItemStack(Items.BEEF);
            return meatStack;
        }
        if (plantStack == null) plantStack = new ItemStack(Items.KELP);
        return plantStack;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setMeat(tag.getBoolean("Meat"));
        if (tag.contains("Size")) setSize(tag.getFloat("Size"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Meat", isMeat());
        tag.putFloat("Size", getSize());
    }

    /** Seen from far away too (for big cells the camera is high above). */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }
}
