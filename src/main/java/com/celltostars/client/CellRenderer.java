package com.celltostars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.celltostars.CellToStars;
import com.celltostars.data.CellLook;
import com.celltostars.data.Diet;
import com.celltostars.data.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Draws a cell: a blocky body (tinted with the primary colour), a pattern on top (secondary colour),
 * two eyes, the mouth that matches the diet, and the bought parts (made of tiny Minecraft blocks).
 *
 * Model space (like every Minecraft entity model): 1 unit = 1 block, origin = middle of the body,
 * -Y = up (towards the camera), -Z = FRONT (mouth, spikes), +Z = BACK (flagella), X = sideways.
 * The body is 8 px wide, ~7 px tall and 10 px long.
 */
public final class CellRenderer {
    /** Model units -> world size: at entity scale 0.3 the cell is about 0.2 blocks long. */
    public static final float SIZE_PER_SCALE = 1.07f;
    /** The middle of the body is this high above the entity's feet (in model px, scaled with the cell). */
    public static final float CENTRE_HEIGHT = 3.5f / 16f;

    private static final ResourceLocation BASE = CellToStars.id("textures/entity/cell_base.png");

    private static ModelPart body;
    private static ModelPart eyes;
    private static ModelPart tube;
    private static ModelPart tubeTip;
    private static ItemStack tail, spike, membrane, feeler, claw;

    private CellRenderer() {
    }

    private static void init() {
        if (body != null) return;
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4f, -2.5f, -5f, 8, 5, 10)    // main blob
                        .texOffs(0, 16).addBox(-3f, -3.5f, -4f, 6, 1, 8)    // rounded top
                        .texOffs(28, 16).addBox(-3f, 2.5f, -4f, 6, 1, 8),   // rounded bottom
                PartPose.ZERO);
        root.addOrReplaceChild("eyes", CubeListBuilder.create()
                        .texOffs(36, 0).addBox(-3.0f, -4.5f, -4.6f, 2, 2, 2)
                        .texOffs(36, 0).addBox(1.0f, -4.5f, -4.6f, 2, 2, 2),
                PartPose.ZERO);
        // omnivore proboscis: a tapering tube sticking out of the front, with a flared tip
        root.addOrReplaceChild("tube", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0f, -1.0f, -3.0f, 2.0f, 2.0f, 3.0f)
                        .texOffs(0, 0).addBox(-0.75f, -0.75f, -5.0f, 1.5f, 1.5f, 2.0f),
                PartPose.offset(0, 0.5f, -4.5f));
        root.addOrReplaceChild("tube_tip", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.25f, -1.25f, -5.75f, 2.5f, 2.5f, 1.0f),
                PartPose.offset(0, 0.5f, -4.5f));
        ModelPart model = LayerDefinition.create(mesh, 64, 32).bakeRoot();
        body = model.getChild("body");
        eyes = model.getChild("eyes");
        tube = model.getChild("tube");
        tubeTip = model.getChild("tube_tip");

        tail = new ItemStack(Items.LIME_WOOL);
        spike = new ItemStack(Items.END_ROD);
        membrane = new ItemStack(Items.SLIME_BLOCK);
        feeler = new ItemStack(Items.MOSS_BLOCK);
        claw = new ItemStack(Items.BONE_BLOCK);
    }

    private static int darker(int rgb) {
        int r = (int) (((rgb >> 16) & 0xFF) * 0.8f);
        int g = (int) (((rgb >> 8) & 0xFF) * 0.8f);
        int b = (int) ((rgb & 0xFF) * 0.8f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static ResourceLocation patternTexture(Pattern pattern) {
        return CellToStars.id("textures/entity/cell_pattern_" + pattern.id() + ".png");
    }

    /**
     * Draws a whole cell. The pose must be at the entity's feet, already turned to face the right way
     * (see {@link #setupPose}). {@code overlay} is the red "hurt" flash.
     */
    public static void render(CellLook look, PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                              float age, Level level, int seed) {
        init();
        // soft "breathing" squash
        float squash = 1.0f + Mth.sin(age * 0.15f) * 0.03f;
        pose.pushPose();
        pose.scale(squash, 1.0f / squash, squash);

        body.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(BASE)), light, overlay, 0xFF000000 | look.primary());
        if (look.pattern() != Pattern.NONE) {
            pose.pushPose();
            pose.scale(1.003f, 1.003f, 1.003f); // a hair bigger, so it never flickers against the body
            body.render(pose, buffers.getBuffer(RenderType.entityTranslucent(patternTexture(look.pattern()))),
                    light, overlay, 0xFF000000 | look.secondary());
            pose.popPose();
        }
        eyes.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(BASE)), light, overlay, 0xFFFFFFFF);
        if (look.diet() == Diet.OMNIVORE) {
            // the tube has the body colour (a bit darker), the tip has the pattern colour; it sways slowly
            float sway = Mth.sin(age * 0.2f) * 0.15f;
            tube.yRot = sway;
            tubeTip.yRot = sway;
            tube.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(BASE)), light, overlay, darker(look.primary()));
            tubeTip.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(BASE)), light, overlay, 0xFF000000 | look.secondary());
        }
        pose.popPose();

        drawParts(look, pose, buffers, light, age, level, seed);
    }

    /** Moves the pose from the entity's feet to the middle of the body, turns it and scales it. */
    public static void setupPose(PoseStack pose, float entityScale, float bodyYaw) {
        float s = entityScale * SIZE_PER_SCALE;
        pose.translate(0, CENTRE_HEIGHT * s, 0);
        pose.mulPose(Axis.YP.rotationDegrees(180.0f - bodyYaw));
        pose.scale(-s, -s, s);
    }

    // ---------------- parts ----------------

    private static final float FRONT = 5f / 16f;  // z of the front face
    private static final float BACK = 5f / 16f;   // z of the back face (positive side)

    private static void drawParts(CellLook look, PoseStack pose, MultiBufferSource buffers, int light,
                                  float age, Level level, int seed) {
        Drawer d = new Drawer(pose, buffers, light, level, seed);

        // ---- mouth, depends on the diet ----
        if (look.diet() == Diet.HERBIVORE) {
            // three soft feelers that sway, like Spore's filter mouth
            for (int k = -1; k <= 1; k++) {
                pose.pushPose();
                pose.translate(0, 0, -FRONT);
                pose.mulPose(Axis.YP.rotationDegrees(k * 32 + Mth.sin(age * 0.25f + k * 1.7f) * 7));
                for (int i = 0; i < 4; i++) {
                    d.cube(feeler, 0, 0, -(0.035 + i * 0.06), 0.08f - i * 0.013f);
                }
                pose.popPose();
            }
        } else if (look.diet() == Diet.CARNIVORE) {
            // two bone pincers at the corners of the mouth that snap open and shut
            float open = 18 + Mth.sin(age * 0.35f) * 12;
            for (int side = -1; side <= 1; side += 2) {
                pose.pushPose();
                pose.translate(side * 0.13, 0, -FRONT);
                pose.mulPose(Axis.YP.rotationDegrees(-side * open));
                d.cube(claw, 0, 0, -0.07, 0.11f);
                d.cube(claw, -side * 0.035, 0, -0.15, 0.08f); // tip curls inwards
                pose.popPose();
            }
        }
        // (omnivore: the proboscis is part of the body model, see render())

        // ---- spikes at the front, fanned out to the sides of the mouth ----
        float[] spikeAngles = {-40, 40, 0};
        for (int k = 0; k < look.spikes() && k < spikeAngles.length; k++) {
            pose.pushPose();
            pose.translate(0, 0, -FRONT + 0.03);
            pose.mulPose(Axis.YP.rotationDegrees(spikeAngles[k]));
            pose.translate(0, 0, -0.17);
            pose.mulPose(Axis.XP.rotationDegrees(-90)); // rod along -Z, base against the body
            pose.scale(0.7f, 0.7f, 0.7f);
            d.item(spike);
            pose.popPose();
        }

        // ---- flagella at the back, swinging side to side ----
        int tails = look.flagella();
        for (int k = 0; k < tails; k++) {
            float angle = (k - (tails - 1) / 2.0f) * 22f;
            pose.pushPose();
            pose.translate(0, 0, BACK);
            pose.mulPose(Axis.YP.rotationDegrees(angle));
            for (int i = 0; i < 4; i++) {
                float wave = Mth.sin(age * 0.6f - i * 0.9f + k * 1.3f) * 0.035f * (i + 1);
                d.cube(tail, wave, 0, 0.06 + i * 0.11, 0.26f - i * 0.05f);
            }
            pose.popPose();
        }

        // ---- membrane last, because it is see-through ----
        if (look.membrane()) {
            pose.pushPose();
            pose.scale(1.25f, 0.95f, 1.45f);
            d.item(membrane);
            pose.popPose();
        }
    }

    /** Small helper so the drawing code above stays short. */
    private record Drawer(PoseStack pose, MultiBufferSource buffers, int light, Level level, int seed) {
        void item(ItemStack stack) {
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light,
                    OverlayTexture.NO_OVERLAY, pose, buffers, level, seed);
        }

        /** A tiny cube of a block at (x, y, z); size 1 = half a block (the item "fixed" view is half size). */
        void cube(ItemStack stack, double x, double y, double z, float size) {
            pose.pushPose();
            pose.translate(x, y, z);
            pose.scale(size, size, size);
            item(stack);
            pose.popPose();
        }
    }
}
