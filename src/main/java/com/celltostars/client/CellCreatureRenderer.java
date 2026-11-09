package com.celltostars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.celltostars.entity.CellCreature;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** AI cells are drawn with the same cell renderer as the player, using their species' look. */
public class CellCreatureRenderer extends EntityRenderer<CellCreature> {

    public CellCreatureRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(CellCreature cell, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float bodyYaw = Mth.rotLerp(partialTick, cell.yBodyRotO, cell.yBodyRot);
        float scale = cell.getScale();
        if (cell.deathTime > 0) {
            scale *= Math.max(0.05f, 1.0f - (cell.deathTime + partialTick) / 20.0f); // shrinks away when it dies
        }
        pose.pushPose();
        CellRenderer.setupPose(pose, scale, bodyYaw);
        var species = cell.species();
        var look = species == com.celltostars.entity.Species.KIN ? ClientEvoData.get().look() : species.look;
        pose.scale(species.bodyWidth, 1.0f, species.bodyLength); // each species has its own body shape
        CellRenderer.render(look, pose, buffers, light, LivingEntityRenderer.getOverlayCoords(cell, 0.0f),
                cell.tickCount + partialTick, cell.level(), cell.getId());
        pose.popPose();
        super.render(cell, entityYaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(CellCreature cell) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
