package com.celltostars.data;

import com.celltostars.CellToStars;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** Stores EvoData on the player and saves it with the world. */
@SuppressWarnings("UnstableApiUsage")
public final class EvoAttachments {
    public static final AttachmentType<EvoData> SPORE = AttachmentRegistry.<EvoData>builder()
            .persistent(EvoData.CODEC)
            .copyOnDeath()
            .initializer(() -> EvoData.EMPTY)
            .buildAndRegister(CellToStars.id("evo_data"));

    private EvoAttachments() {
    }

    public static void init() {
        // loading the class registers the attachment
    }
}
