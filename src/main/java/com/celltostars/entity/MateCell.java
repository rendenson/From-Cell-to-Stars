package com.celltostars.entity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.joml.Vector3f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/**
 * The partner that appears when you call for a mate. It looks like you (your skin's head)
 * and gives off hearts. Swim into it to open the editor.
 */
public class MateCell extends Entity implements ItemSupplier {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(MateCell.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final int LIFETIME = 20 * 180;
    private static final DustParticleOptions LOVE_SPARKLE = new DustParticleOptions(new Vector3f(1.0f, 0.45f, 0.75f), 0.4f);

    private ItemStack headStack;

    public MateCell(EntityType<? extends MateCell> type, Level level) {
        super(type, level);
    }

    public void setOwner(UUID owner) {
        this.entityData.set(OWNER, Optional.of(owner));
    }

    public Optional<UUID> getOwner() {
        return this.entityData.get(OWNER);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            Player owner = getOwner().map(level()::getPlayerByUUID).orElse(null);
            if (owner == null || tickCount > LIFETIME) {
                discard();
                return;
            }
            if (tickCount % 40 == 0) {
                setDeltaMovement((random.nextDouble() - 0.5) * 0.01, 0, (random.nextDouble() - 0.5) * 0.01);
            }
            if (level() instanceof ServerLevel server) {
                // small pink sparkles instead of big hearts
                if (tickCount % 6 == 0) {
                    double k = owner.getScale() / 0.3;
                    DustParticleOptions sparkle = new DustParticleOptions(new Vector3f(1.0f, 0.45f, 0.75f), (float) Math.min(4.0, 0.4 * k));
                    for (ServerPlayer viewer : server.players()) {
                        server.sendParticles(viewer, sparkle, true, getX(), getY() + 0.1 * k, getZ(), 2, 0.12 * k, 0.02, 0.12 * k, 0.0);
                    }
                }
                // answers the call now and then, so you can also find it by ear
                if (tickCount % 80 == 30) {
                    server.playSound(null, getX(), getY(), getZ(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.NEUTRAL,
                            (float) (2.0 * owner.getScale() / 0.3), 1.25f); // louder = heard from further away
                }
            }
        }
        move(MoverType.SELF, getDeltaMovement());
    }

    /** Drawn as a player head with the owner's skin. */
    @Override
    public ItemStack getItem() {
        if (headStack == null) {
            ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
            Player owner = getOwner().map(level()::getPlayerByUUID).orElse(null);
            if (owner == null) return stack; // owner not known yet, try again next frame
            stack.set(DataComponents.PROFILE, new ResolvableProfile(owner.getGameProfile()));
            headStack = stack;
        }
        return headStack;
    }

    @Override
    public boolean shouldBeSaved() {
        return false; // a new mate is called every time
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
