package com.celltostars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.celltostars.CellToStars;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Cell stage: a small flat health bar over every creature that is hurt.
 * The camera looks straight down, so the bar lies flat (in the X/Z plane) just above the creature.
 */
public final class HealthBars {
    private HealthBars() {
    }

    public static void render(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !ClientEvoData.isCell() || mc.level.dimension() != CellToStars.CELL_LEVEL) return;
        PoseStack pose = context.matrixStack();
        MultiBufferSource buffers = context.consumers();
        if (pose == null || buffers == null) return;

        Vec3 cam = context.camera().getPosition();
        float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(false);
        VertexConsumer quads = buffers.getBuffer(RenderType.debugQuads());

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || entity instanceof Player || !living.isAlive()) continue;
            float health = living.getHealth();
            float max = living.getMaxHealth();
            if (health >= max || max <= 0) continue;

            Vec3 pos = entity.getPosition(partialTick);
            float width = Math.max(0.22f, entity.getBbWidth() * 1.3f);
            float half = width / 2;
            float thick = 0.035f;
            float filled = width * Math.max(0, health / max);
            // bar sits just "north" of the creature (= above it on the screen)
            float z0 = -entity.getBbWidth() / 2 - 0.08f;

            pose.pushPose();
            pose.translate(pos.x - cam.x, pos.y + entity.getBbHeight() + 0.05 - cam.y, pos.z - cam.z);
            Matrix4f m = pose.last().pose();
            flatQuad(quads, m, -half - 0.01f, half + 0.01f, z0 - thick - 0.01f, z0 + 0.01f, 0.0f, 0xFF000000);
            flatQuad(quads, m, -half, half, z0 - thick, z0, 0.002f, 0xFF4A1010);
            flatQuad(quads, m, -half, -half + filled, z0 - thick, z0, 0.004f, 0xFFE03030);
            pose.popPose();
        }
    }

    /** A flat rectangle, drawn from both sides so it is never culled away. */
    private static void flatQuad(VertexConsumer c, Matrix4f m, float x0, float x1, float z0, float z1, float y, int argb) {
        int a = (argb >>> 24) & 0xFF, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        c.addVertex(m, x0, y, z0).setColor(r, g, b, a);
        c.addVertex(m, x0, y, z1).setColor(r, g, b, a);
        c.addVertex(m, x1, y, z1).setColor(r, g, b, a);
        c.addVertex(m, x1, y, z0).setColor(r, g, b, a);

        c.addVertex(m, x1, y, z0).setColor(r, g, b, a);
        c.addVertex(m, x1, y, z1).setColor(r, g, b, a);
        c.addVertex(m, x0, y, z1).setColor(r, g, b, a);
        c.addVertex(m, x0, y, z0).setColor(r, g, b, a);
    }
}
