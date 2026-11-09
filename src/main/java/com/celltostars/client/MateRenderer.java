package com.celltostars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.celltostars.entity.MateCell;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** The mate looks exactly like your cell: same body, colours, pattern, mouth and parts, same size. */
public class MateRenderer extends EntityRenderer<MateCell> {

    public MateRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MateCell mate, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = mate.tickCount + partialTick;
        Player owner = mate.getOwner().map(mate.level()::getPlayerByUUID).orElse(null);
        float scale = owner != null ? owner.getScale() : 0.3f;

        pose.pushPose();
        pose.translate(0, -0.15 + Mth.sin(age * 0.1f) * 0.01, 0); // the entity floats 0.15 above the plane
        CellRenderer.setupPose(pose, scale, Mth.rotLerp(partialTick, mate.yRotO, mate.getYRot()));
        CellRenderer.render(ClientEvoData.get().look(), pose, buffers, light, OverlayTexture.NO_OVERLAY,
                age, mate.level(), mate.getId());
        pose.popPose();

        super.render(mate, entityYaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(MateCell mate) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
