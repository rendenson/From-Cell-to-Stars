package com.celltostars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.celltostars.entity.FoodParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

/** Food is a flat item picture (kelp / beef) facing the camera; it grows with the cells that eat it. */
public class FoodRenderer extends EntityRenderer<FoodParticle> {
    private final ItemRenderer itemRenderer;

    public FoodRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(FoodParticle food, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float size = 0.6f * food.getSize();
        pose.pushPose();
        pose.translate(0, 0.1 * food.getSize(), 0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.ZP.rotationDegrees((food.getId() * 37) % 360)); // every piece turned a bit differently
        pose.scale(size, size, size);
        itemRenderer.renderStatic(food.getItem(), ItemDisplayContext.GROUND, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, pose, buffers, food.level(), food.getId());
        pose.popPose();
        super.render(food, entityYaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(FoodParticle food) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
