package com.celltostars.logic;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Runs an action for a player a few ticks later.
 * Needed because teleporting a player to another world in the same moment they join or respawn
 * leaves them "half in" the old world (the server can't find them next to food etc.).
 */
public final class EvoScheduler {
    private record Task(UUID player, int[] ticksLeft, Consumer<ServerPlayer> action) {
    }

    private static final List<Task> TASKS = new ArrayList<>();

    private EvoScheduler() {
    }

    public static void later(ServerPlayer player, int ticks, Consumer<ServerPlayer> action) {
        TASKS.add(new Task(player.getUUID(), new int[]{ticks}, action));
    }

    public static void tick(MinecraftServer server) {
        if (TASKS.isEmpty()) return;
        List<Task> ready = new ArrayList<>();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            if (--task.ticksLeft()[0] <= 0) {
                it.remove();
                ready.add(task);
            }
        }
        for (Task task : ready) {
            ServerPlayer player = server.getPlayerList().getPlayer(task.player());
            if (player != null) task.action().accept(player);
        }
    }
}
