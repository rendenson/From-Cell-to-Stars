package com.celltostars.logic;

import com.celltostars.CellToStars;
import com.celltostars.data.EvoData;
import com.celltostars.data.Stage;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Hooks the mod into game events (join, respawn, ticks, deaths). */
public final class EvoEvents {
    private EvoEvents() {
    }

    public static void init() {
        // when a player joins: start as a cell in a new world, otherwise re-apply size/health and sync
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> EvoLogic.onJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> EvoCycle.onDisconnect(handler.getPlayer()));

        // a cell can't hit, break, place or use anything - it only touches things with its body
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
                EvoLogic.isCell(player) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
                EvoLogic.isCell(player) ? InteractionResult.FAIL : InteractionResult.PASS);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                EvoLogic.isCell(player) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                EvoLogic.isCell(player) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseItemCallback.EVENT.register((player, world, hand) -> EvoLogic.isCell(player)
                ? InteractionResultHolder.fail(player.getItemInHand(hand))
                : InteractionResultHolder.pass(player.getItemInHand(hand)));

        // after death a cell respawns in the soup, not in the overworld
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            EvoData data = EvoLogic.get(newPlayer);
            EvoLogic.applyAttributes(newPlayer, data);
            EvoLogic.sync(newPlayer, data);
            if (data.stage() == Stage.CELL) { // (before, every death sent you back to the middle of the soup)
                // where the old cell died (in the soup), so the new one hatches far from its killers
                var death = oldPlayer.getLastDeathLocation()
                        .filter(pos -> pos.dimension() == CellToStars.CELL_LEVEL).map(pos -> pos.pos());
                EvoScheduler.later(newPlayer, 5, p -> {
                    boolean moved = death.isPresent()
                            ? EvoLogic.teleportToSoupAwayFrom(p, death.get().getX(), death.get().getZ())
                            : EvoLogic.teleportToSoup(p);
                    if (moved) {
                        p.setHealth(p.getMaxHealth());
                        EvoCycle.hatchScene(p, false); // born again from an egg, like in Spore
                    }
                });
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(EvoScheduler::tick);

        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.dimension() == CellToStars.CELL_LEVEL) {
                EvoLogic.tickSoup(level);
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof Player) return;
            if (!(entity.level() instanceof ServerLevel level) || level.dimension() != CellToStars.CELL_LEVEL) return;
            if (source.getEntity() instanceof ServerPlayer killer && EvoLogic.get(killer).stage() == Stage.CELL) {
                EvoLogic.onCreatureKilled(level, entity);
            }
        });
    }
}
