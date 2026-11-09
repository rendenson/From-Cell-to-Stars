package com.celltostars.logic;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.celltostars.CellToStars;
import com.celltostars.data.EvoData;
import com.celltostars.data.Stage;
import com.celltostars.entity.CellCreature;
import com.celltostars.entity.Species;
import com.celltostars.network.EvoNetwork;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

/**
 * Cheat commands for testing (need cheats / op):
 * /evo dna <amount>              - add DNA
 * /evo level <1-5>               - jump to a growth level
 * /evo heal                      - full health
 * /evo food                      - a ring of food around you
 * /evo spawn <species> [level]   - an AI cell right in front of you (tab completes the species)
 * /evo clear                     - remove all AI cells nearby
 * /evo mate                      - a mate right next to you
 * /evo editor                    - jump straight into the editor
 * /evo evolve                    - fill the bar and evolve right away
 */
public final class EvoCommands {
    private static final String[] SPECIES_IDS = Arrays.stream(Species.values()).map(s -> s.id).toArray(String[]::new);

    private EvoCommands() {
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("evo")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("dna")
                                .then(Commands.argument("amount", IntegerArgumentType.integer()).executes(ctx -> {
                                    EvoLogic.addDna(ctx.getSource().getPlayerOrException(),
                                            IntegerArgumentType.getInteger(ctx, "amount"));
                                    return 1;
                                })))
                        .then(Commands.literal("level")
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, EvoData.MAX_LEVEL)).executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    EvoLogic.set(player, EvoLogic.get(player).atLevel(IntegerArgumentType.getInteger(ctx, "level")));
                                    return 1;
                                })))
                        .then(Commands.literal("heal").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            player.setHealth(player.getMaxHealth());
                            return 1;
                        }))
                        .then(Commands.literal("food").executes(ctx -> {
                            EvoLogic.spawnFoodBurst(ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("species", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SPECIES_IDS, builder))
                                        .executes(ctx -> spawn(ctx.getSource(), StringArgumentType.getString(ctx, "species"), -1))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 7))
                                                .executes(ctx -> spawn(ctx.getSource(), StringArgumentType.getString(ctx, "species"),
                                                        IntegerArgumentType.getInteger(ctx, "level"))))))
                        .then(Commands.literal("clear").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            var cells = player.level().getEntitiesOfClass(CellCreature.class, player.getBoundingBox().inflate(64));
                            cells.forEach(CellCreature::discard);
                            ctx.getSource().sendSuccess(() -> Component.literal("Removed " + cells.size() + " cells"), false);
                            return cells.size();
                        }))
                        .then(Commands.literal("mate").executes(ctx -> {
                            EvoCycle.callMateNearby(ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("evolve").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            EvoLogic.addDna(player, 100);
                            EvoLogic.evolve(player, true);
                            return 1;
                        }))
                        .then(Commands.literal("editor").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (EvoLogic.get(player).stage() == Stage.CELL && !EvoCycle.isInEgg(player)) {
                                EvoCycle.enterEgg(player, EvoNetwork.EDITOR_MATE);
                            }
                            return 1;
                        }))
        ));
    }

    private static int spawn(CommandSourceStack source, String speciesId, int level) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Species species = Species.byId(speciesId);
        if (species == null) {
            source.sendFailure(Component.literal("Unknown species: " + speciesId + " (use: " + String.join(", ", SPECIES_IDS) + ")"));
            return 0;
        }
        if (player.level().dimension() != CellToStars.CELL_LEVEL) {
            source.sendFailure(Component.literal("Only works in the primordial soup"));
            return 0;
        }
        // its own fixed size, or a level you ask for (1-6); your own kind is your size
        int tier = level > 0 ? 2 * level : species == Species.KIN ? EvoLogic.get(player).tier() : species.tier;
        EvoLogic.spawnCreatureInFront(player, species, tier);
        return 1;
    }
}
