package com.celltostars.logic;

import com.celltostars.CellToStars;
import com.celltostars.data.Diet;
import com.celltostars.data.Part;
import com.celltostars.data.EvoData;
import com.celltostars.data.Stage;
import com.celltostars.entity.HatchShell;
import com.celltostars.entity.MateCell;
import com.celltostars.entity.EvoEntities;
import com.celltostars.network.EvoNetwork;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import org.joml.Vector3f;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Spore life cycle of a cell: call a mate -> find it -> love scene -> editor (inside an egg) -> hatch somewhere new.
 * The editor is also shown when a world is created and every time you join it again.
 */
public final class EvoCycle {
    private static final double PLANE_Y = CellToStars.CELL_PLANE_Y;
    private static final int MAX_NAME_LENGTH = 24;

    private static final DustParticleOptions WAVE = new DustParticleOptions(new Vector3f(1.0f, 0.55f, 0.85f), 0.35f);

    /** Players who are inside their egg right now (editor open). They can't be hurt and don't eat. */
    private static final Set<UUID> IN_EGG = new HashSet<>();
    /** Players whose egg is on screen and cracking right now. */
    private static final Set<UUID> HATCHING = new HashSet<>();
    /** Players who will hatch for the first time (from a meteorite instead of an egg). */
    private static final Set<UUID> FIRST_HATCH = new HashSet<>();
    /** player -> their mate entity */
    private static final Map<UUID, UUID> MATES = new HashMap<>();

    private EvoCycle() {
    }

    public static boolean isInEgg(ServerPlayer player) {
        return IN_EGG.contains(player.getUUID());
    }

    // ---------- egg & editor ----------

    public static void enterEgg(ServerPlayer player, int editorMode) {
        IN_EGG.add(player.getUUID());
        if (editorMode == EvoNetwork.EDITOR_CREATE) FIRST_HATCH.add(player.getUUID());
        player.setInvulnerable(true);
        player.setDeltaMovement(Vec3.ZERO);
        removeMate(player);
        ServerPlayNetworking.send(player, new EvoNetwork.OpenEditorPayload(editorMode));
    }

    /**
     * "Done" in the editor: the cell appears in a new random place inside an egg (or a meteorite the
     * very first time), which cracks open after a moment. Full health after hatching.
     */
    public static void hatch(ServerPlayer player, String name) {
        UUID id = player.getUUID();
        if (!IN_EGG.contains(id) || HATCHING.contains(id)) return;
        HATCHING.add(id);
        boolean meteor = FIRST_HATCH.remove(id);

        rename(player, name);
        player.setHealth(player.getMaxHealth());
        HATCHING.remove(id); // hatchScene sets it again

        if (player.level() instanceof ServerLevel level && level.dimension() == CellToStars.CELL_LEVEL) {
            RandomSource random = level.getRandom();
            double k = player.getScale() / 0.3;
            double x = player.getX() + (random.nextDouble() * 2 - 1) * 150 * k;
            double z = player.getZ() + (random.nextDouble() * 2 - 1) * 150 * k;
            player.teleportTo(level, x, PLANE_Y, z, 180.0f, 0.0f);
        }
        hatchScene(player, meteor);
    }

    /**
     * The cell sits inside an egg (or a meteorite) for a moment, then it cracks open.
     * Used after the editor and after respawning. The player is safe and frozen meanwhile.
     */
    public static void hatchScene(ServerPlayer player, boolean meteor) {
        UUID id = player.getUUID();
        IN_EGG.add(id);
        HATCHING.add(id);
        player.setInvulnerable(true);
        ServerPlayNetworking.send(player, EvoNetwork.HatchScenePayload.INSTANCE);

        HatchShell shell = null;
        if (player.level() instanceof ServerLevel level) {
            shell = EvoEntities.HATCH_SHELL.create(level);
            if (shell != null) {
                shell.setMeteor(meteor);
                shell.setSize(player.getScale() / 0.3f);
                shell.moveTo(player.getX(), PLANE_Y + 0.02, player.getZ(), 0, 0);
                level.addFreshEntity(shell);
            }
            level.playSound(null, player.blockPosition(),
                    meteor ? SoundEvents.FIRECHARGE_USE : SoundEvents.TURTLE_EGG_CRACK,
                    SoundSource.PLAYERS, meteor ? 0.4f : 1.0f, meteor ? 1.6f : 1.0f);
        }

        if (meteor) {
            EvoScheduler.later(player, 14, p -> {
                if (!(p.level() instanceof ServerLevel level)) return;
                level.sendParticles(ParticleTypes.BUBBLE_POP, p.getX(), p.getY() + 0.2, p.getZ(), 30, 0.4, 0.05, 0.4, 0.05);
                level.sendParticles(ParticleTypes.LAVA, p.getX(), p.getY() + 0.2, p.getZ(), 6, 0.1, 0.02, 0.1, 0.0);
                level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0f, 0.7f);
            });
        }

        HatchShell finalShell = shell;
        EvoScheduler.later(player, 35, p -> {
            if (finalShell != null) finalShell.discard();
            IN_EGG.remove(p.getUUID());
            HATCHING.remove(p.getUUID());
            p.setInvulnerable(false);
            p.setHealth(p.getMaxHealth());
            if (p.level() instanceof ServerLevel level) {
                hatchEffects(level, p, meteor);
            }
        });
    }

    public static void rename(ServerPlayer player, String name) {
        String cleanName = name.strip();
        if (cleanName.length() > MAX_NAME_LENGTH) cleanName = cleanName.substring(0, MAX_NAME_LENGTH);
        if (cleanName.isEmpty()) cleanName = player.getGameProfile().getName(); // never nameless
        EvoLogic.set(player, EvoLogic.get(player).withName(cleanName));
    }

    /** The shell bursts into pieces. */
    public static void hatchEffects(ServerLevel level, ServerPlayer player, boolean meteor) {
        ItemStack pieces = new ItemStack(meteor ? Items.MAGMA_BLOCK : Items.SNIFFER_EGG);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, pieces),
                player.getX(), player.getY() + 0.2, player.getZ(), 16, 0.15, 0.05, 0.15, 0.06);
        level.playSound(null, player.blockPosition(), SoundEvents.TURTLE_EGG_HATCH, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    /** Parts can only be changed inside the egg (in the editor). */
    public static void buyPart(ServerPlayer player, Part part) {
        if (!isInEgg(player)) return;
        EvoData data = EvoLogic.get(player);
        if (data.count(part) < part.maxCount() && data.dna() >= part.cost()) {
            EvoLogic.set(player, data.buy(part));
        }
    }

    public static void sellPart(ServerPlayer player, Part part) {
        if (!isInEgg(player)) return;
        EvoData data = EvoLogic.get(player);
        if (data.has(part)) {
            EvoLogic.set(player, data.sell(part));
        }
    }

    /** Painting is free, like in Spore. */
    public static void setLook(ServerPlayer player, int primary, int secondary, String pattern) {
        if (!isInEgg(player)) return;
        EvoLogic.set(player, EvoLogic.get(player).withLook(primary & 0xFFFFFF, secondary & 0xFFFFFF,
                com.celltostars.data.Pattern.byId(pattern).id()));
    }

    public static void setDiet(ServerPlayer player, Diet diet) {
        if (!isInEgg(player)) return;
        EvoLogic.set(player, EvoLogic.get(player).withDiet(diet));
    }

    // ---------- mate ----------

    public static void callMate(ServerPlayer player) {
        if (isInEgg(player) || EvoLogic.get(player).stage() != Stage.CELL) return;
        if (!(player.level() instanceof ServerLevel level) || level.dimension() != CellToStars.CELL_LEVEL) return;
        if (hasMate(player)) return; // one mate at a time - G does nothing until it is gone

        MateCell mate = EvoEntities.MATE.create(level);
        if (mate == null) return;
        RandomSource random = level.getRandom();
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = (9 + random.nextDouble() * 6) * player.getScale() / 0.3;
        mate.setOwner(player.getUUID());
        mate.moveTo(player.getX() + Math.cos(angle) * distance, PLANE_Y + 0.15, player.getZ() + Math.sin(angle) * distance,
                random.nextFloat() * 360f, 0);
        level.addFreshEntity(mate);
        MATES.put(player.getUUID(), mate.getUUID());

        // a long, melodic "where are you?" call (the mate answers from time to time, see MateCell)
        level.playSound(null, player.blockPosition(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.PLAYERS, 2.0f, 0.9f);
        level.playSound(null, player.blockPosition(), SoundEvents.DOLPHIN_AMBIENT_WATER, SoundSource.PLAYERS, 1.5f, 1.3f);
        player.sendSystemMessage(Component.translatable("celltostars.msg.mate_called").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Test command: the mate appears right next to you. */
    public static void callMateNearby(ServerPlayer player) {
        removeMate(player);
        callMate(player);
        UUID mateId = MATES.get(player.getUUID());
        if (mateId != null && player.level() instanceof ServerLevel level) {
            Entity mate = level.getEntity(mateId);
            if (mate != null) mate.moveTo(player.getX() + 0.8 * player.getScale() / 0.3, mate.getY(), player.getZ());
        }
    }

    public static boolean hasMate(ServerPlayer player) {
        return MATES.containsKey(player.getUUID());
    }

    private static void removeMate(ServerPlayer player) {
        UUID mateId = MATES.remove(player.getUUID());
        if (mateId != null && player.level() instanceof ServerLevel level) {
            Entity mate = level.getEntity(mateId);
            if (mate != null) mate.discard();
        }
    }

    /** Every tick for each cell: waves that point to the mate, and the love scene when you reach it. */
    public static void tickMate(ServerLevel level, ServerPlayer player) {
        UUID mateId = MATES.get(player.getUUID());
        if (mateId == null) return;
        Entity mate = level.getEntity(mateId);
        if (mate == null || !mate.isAlive()) {
            MATES.remove(player.getUUID());
            return;
        }

        Vec3 toMate = new Vec3(mate.getX() - player.getX(), 0, mate.getZ() - player.getZ());
        double distance = toMate.length();
        if (distance < 0.45 * player.getScale() / 0.3) {
            startLove(level, player, mate);
            return;
        }

        // small pink "waves" (>-shaped chevrons) that roll away from you towards the mate
        long t = level.getGameTime();
        if (t % 4 == 0) {
            Vec3 dir = toMate.normalize();
            Vec3 side = new Vec3(-dir.z, 0, dir.x);
            double k = player.getScale() / 0.3;     // everything grows with the cell
            DustParticleOptions wave = new DustParticleOptions(new Vector3f(1.0f, 0.55f, 0.85f), (float) Math.min(4.0, 0.35 * k));
            double d = (0.45 + ((t / 4) % 4) * 0.28) * k; // 4 waves one after another, then repeat
            for (int j = -3; j <= 3; j++) {
                double back = Math.abs(j) * 0.045 * k;   // the wings bend back -> arrow-like wave
                level.sendParticles(player, wave, true,
                        player.getX() + dir.x * (d - back) + side.x * j * 0.05 * k,
                        player.getY() + 0.15 * k,
                        player.getZ() + dir.z * (d - back) + side.z * j * 0.05 * k,
                        1, 0, 0, 0, 0);
            }
        }
    }

    private static void startLove(ServerLevel level, ServerPlayer player, Entity mate) {
        MATES.remove(player.getUUID());
        level.sendParticles(ParticleTypes.HEART, mate.getX(), mate.getY() + 0.2, mate.getZ(), 12, 0.3, 0.05, 0.3, 0.0);
        mate.discard();
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.8f);

        ServerPlayNetworking.send(player, EvoNetwork.LovePayload.INSTANCE);
        IN_EGG.add(player.getUUID()); // frozen and safe during the scene
        player.setInvulnerable(true);
        EvoScheduler.later(player, 40, p -> enterEgg(p, EvoNetwork.EDITOR_MATE));
    }

    /** Pressing "Evolve" in the editor: leave the egg without the hatching scene. */
    public static void leaveEggForEvolution(ServerPlayer player) {
        IN_EGG.remove(player.getUUID());
        HATCHING.remove(player.getUUID());
        FIRST_HATCH.remove(player.getUUID());
        player.setInvulnerable(false);
    }

    public static void onDisconnect(ServerPlayer player) {
        removeMate(player);
        HATCHING.remove(player.getUUID());
        FIRST_HATCH.remove(player.getUUID());
        if (IN_EGG.remove(player.getUUID())) {
            player.setInvulnerable(false);
        }
    }
}
