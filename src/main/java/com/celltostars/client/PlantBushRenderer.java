package com.celltostars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.celltostars.entity.PlantBush;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Mossy core, branches of green blocks, and a leafy flowering "fruit" at the end of each branch. */
public class PlantBushRenderer extends EntityRenderer<PlantBush> {
    private final ItemRenderer itemRenderer;
    private ItemStack core, branch, fruit;

    public PlantBushRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(PlantBush bush, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (core == null) {
            core = new ItemStack(Items.MOSS_BLOCK);
            branch = new ItemStack(Items.GREEN_TERRACOTTA);
            fruit = new ItemStack(Items.FLOWERING_AZALEA_LEAVES);
        }
        float s = bush.size();
        float age = bush.tickCount + partialTick;
        int n = bush.branches();
        int fullLight = LightTexture.FULL_BRIGHT;

        pose.pushPose();
        pose.translate(0, 0.12 * s, 0);
        cube(core, pose, buffers, fullLight, bush, 0, 0, 0, 0.75f * s, age * 0.3f);

        for (int i = 0; i < n; i++) {
            float angle = (bush.getYRot() + i * 360.0f / n) * Mth.DEG_TO_RAD;
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);
            float sway = Mth.sin(age * 0.05f + i) * 0.03f * s;
            // three small blocks along the branch
            for (int j = 1; j <= 3; j++) {
                double r = bush.branchLength() * j / 4.0;
                cube(branch, pose, buffers, fullLight, bush, cos * r + sway * j, 0, sin * r - sway * j, 0.22f * s, 0);
            }
            if (bush.hasFruit(i)) {
                double r = bush.branchLength();
                float bob = 1.0f + Mth.sin(age * 0.1f + i * 2) * 0.06f;
                cube(fruit, pose, buffers, fullLight, bush, cos * r + sway * 4, 0, sin * r - sway * 4, 0.42f * s * bob, age);
            }
        }
        pose.popPose();
        super.render(bush, entityYaw, partialTick, pose, buffers, light);
    }

    private void cube(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, PlantBush bush,
                      double x, double y, double z, float size, float spin) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        pose.scale(size, size, size);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers,
                bush.level(), bush.getId());
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(PlantBush bush) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
