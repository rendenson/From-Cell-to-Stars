package com.celltostars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.celltostars.entity.HatchShell;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Meteorite: falls from right under the camera, tumbling, slams into the plane, then glows and shakes.
 * Egg: sits there and wobbles more and more until it cracks.
 */
public class HatchShellRenderer extends EntityRenderer<HatchShell> {
    public static final float METEOR_FALL_TICKS = 14f;

    private final BlockRenderDispatcher blockRenderer;

    public HatchShellRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(HatchShell shell, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = shell.tickCount + partialTick;
        pose.pushPose();
        if (shell.isMeteor()) {
            float fall = Math.max(0, 1 - age / METEOR_FALL_TICKS);
            float spin = Math.min(age, METEOR_FALL_TICKS) * 28f;
            float shake = fall > 0 ? 0 : Mth.sin(age * 2.3f) * 0.015f;
            pose.translate(shake, (0.15 + fall * fall * 2.6) * shell.getSize(), -shake);
            pose.mulPose(Axis.YP.rotationDegrees(spin));
            pose.mulPose(Axis.XP.rotationDegrees(spin * 0.6f));
            light = LightTexture.FULL_BRIGHT; // it glows
        } else {
            float wobble = Mth.sin(age * 0.9f) * Math.min(1, age / 30f) * 16f;
            pose.translate(0, 0.12 * shell.getSize(), 0);
            pose.mulPose(Axis.ZP.rotationDegrees(wobble));
            pose.mulPose(Axis.XP.rotationDegrees(wobble * 0.5f));
        }
        // a real 3D block (the sniffer egg *item* is only a flat picture, which looked like a thin stick from above)
        float size = 0.32f * shell.getSize();
        pose.scale(size, size, size);
        pose.translate(-0.5, -0.5, -0.5); // block models go from 0 to 1, we want them centred
        BlockState block = shell.isMeteor() ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.SNIFFER_EGG.defaultBlockState();
        blockRenderer.renderSingleBlock(block, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(shell, entityYaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(HatchShell shell) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
