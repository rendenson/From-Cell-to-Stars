package com.celltostars;

import com.celltostars.data.EvoAttachments;
import com.celltostars.entity.EvoEntities;
import com.celltostars.logic.EvoCommands;
import com.celltostars.logic.EvoEvents;
import com.celltostars.network.EvoNetwork;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CellToStars implements ModInitializer {
    public static final String MOD_ID = "celltostars";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** The "primordial soup" world where the cell stage happens (data/celltostars/dimension/primordial_soup.json). */
    public static final ResourceKey<Level> CELL_LEVEL = ResourceKey.create(Registries.DIMENSION, id("primordial_soup"));

    /**
     * Cells live on one flat layer in the middle of the very deep water (water is y=4..239),
     * so it feels like floating. The sand floor is far below, lost in the murk; giant cells of
     * bigger layers swim down there as dark shapes. Deep enough that even a level-5 cell's camera stays under water.
     */
    public static final double CELL_PLANE_Y = 150.0;

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        EvoSounds.init();
        EvoAttachments.init();
        EvoEntities.init();
        EvoNetwork.init();
        EvoCommands.init();
        EvoEvents.init();
        LOGGER.info("Spore mod loaded");
    }
}
