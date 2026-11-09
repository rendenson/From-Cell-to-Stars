package com.celltostars.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * What the cell hatches from: an egg after the editor, or a hot meteorite the very first time
 * (in Spore life arrives on a meteor). It sits around the cell for a moment, then cracks.
 */
public class HatchShell extends Entity implements ItemSupplier {
    private static final EntityDataAccessor<Boolean> METEOR = SynchedEntityData.defineId(HatchShell.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(HatchShell.class, EntityDataSerializers.FLOAT);

    private ItemStack eggStack;
    private ItemStack meteorStack;

    public HatchShell(EntityType<? extends HatchShell> type, Level level) {
        super(type, level);
    }

    public boolean isMeteor() {
        return this.entityData.get(METEOR);
    }

    public void setMeteor(boolean meteor) {
        this.entityData.set(METEOR, meteor);
    }

    /** Grows with the cell inside (1 = level-1 cell). */
    public float getSize() {
        return this.entityData.get(SIZE);
    }

    public void setSize(float size) {
        this.entityData.set(SIZE, size);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(METEOR, false);
        builder.define(SIZE, 1.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            if (tickCount > 100) { // safety: never stay forever
                discard();
                return;
            }
            if (isMeteor() && tickCount > 14 && tickCount % 3 == 0) { // burns after landing
                server.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.15, getZ(), 2, 0.12, 0.02, 0.12, 0.005);
                server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.2, getZ(), 1, 0.1, 0.02, 0.1, 0.01);
            } else if (!isMeteor() && tickCount % 10 == 0) {
                server.sendParticles(ParticleTypes.BUBBLE, getX(), getY() + 0.2, getZ(), 2, 0.12, 0.02, 0.12, 0.0);
            }
        }
    }

    @Override
    public ItemStack getItem() {
        if (isMeteor()) {
            if (meteorStack == null) meteorStack = new ItemStack(Items.MAGMA_BLOCK);
            return meteorStack;
        }
        if (eggStack == null) eggStack = new ItemStack(Items.SNIFFER_EGG);
        return eggStack;
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

    /** Seen from far away too (for big cells the camera is high above). */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }
}
