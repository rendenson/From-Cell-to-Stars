package com.celltostars.client;

import com.celltostars.data.EvoData;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** DNA counter + evolution bar in the top-left corner, and "+3 DNA" popups above the cell. */
public final class EvoHud {
    private static final int BAR_WIDTH = 130;
    private static final long POPUP_MS = 1200;

    private record Popup(int amount, long startMs) {
    }

    private static final List<Popup> POPUPS = new ArrayList<>();

    private EvoHud() {
    }

    public static void onDnaGained(int amount) {
        POPUPS.add(new Popup(amount, Util.getMillis()));
    }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !ClientEvoData.isCell()) {
            POPUPS.clear();
            return;
        }
        renderFades(g);
        if (mc.options.hideGui || ClientEvoData.isInEgg()) return;

        EvoData data = ClientEvoData.get();
        Font font = mc.font;

        // ---- top left: name, diet path ----
        int x = 8;
        int y = 8;
        if (!data.name().isEmpty()) {
            g.drawString(font, data.name(), x, y, 0xFFFFE680);
            y += 12;
        }
        DietPathBar.draw(g, font, x, y, BAR_WIDTH, data);

        // ---- bottom strip, like Spore: DNA | evolution bar in 5 stages | health ----
        renderBottomStrip(g, font, mc, data);
        renderPopups(g, font);
    }

    /** Love scene: the screen goes dark. Hatching: it lights up again. */
    private static void renderFades(GuiGraphics g) {
        long now = Util.getMillis();
        float darkness = 0;
        long love = ClientEvoData.loveStartMs();
        if (love != 0) {
            float t = (now - love) / 2000f;
            darkness = Math.max(0, Math.min(1, (t - 0.4f) / 0.6f));
        }
        long hatch = ClientEvoData.hatchStartMs();
        if (hatch != 0 && now - hatch < 500) {
            darkness = Math.max(darkness, 1 - (now - hatch) / 500f);
        }
        if (darkness > 0.01f) {
            int alpha = (int) (darkness * 255);
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), (alpha << 24) | 0x0A0510);
        }
    }

    /**
     * Spore-style bottom strip across the whole screen: DNA on the left, the evolution bar in the middle
     * (split into the 5 growth stages, the current one highlighted), health on the right, hint above it.
     */
    private static void renderBottomStrip(GuiGraphics g, Font font, Minecraft mc, EvoData data) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        int top = h - 26;
        g.fill(0, top, w, h, 0xB0061218);
        g.fill(0, top, w, top + 1, 0xFF2E6B6B);

        // DNA
        g.fill(4, top + 4, 66, h - 4, 0xFF0B2A2E);
        g.drawString(font, Component.translatable("celltostars.hud.dna", data.dna()), 9, top + 9, 0xFF55FFFF);

        // portrait + health (right), standing on the strip like in Spore
        int panelX = renderPortrait(g, font, mc, w, top);

        // evolution bar (middle), 5 stages
        int barX = 76;
        int barW = panelX - 10 - barX;
        int barY = top + 12;
        int stages = EvoData.MAX_LEVEL;
        int filled = (int) (barW * Math.min(1.0f, data.progress() / (float) EvoData.EVOLVE_AT));
        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + 9, 0xFF000000);
        g.fill(barX, barY, barX + barW, barY + 8, 0xFF16303A);
        int current = data.level() - 1;
        int stageW = barW / stages;
        g.fill(barX + current * stageW, barY, barX + (current + 1) * stageW, barY + 8, 0xFF1F4552); // current stage
        g.fill(barX, barY, barX + filled, barY + 8, data.canEvolve() ? 0xFFFFCC33 : 0xFF46C846);
        g.fill(barX, barY, barX + filled, barY + 2, data.canEvolve() ? 0xFFFFE699 : 0xFF8BE88B);
        for (int i = 1; i < stages; i++) { // stage dividers with small diamonds
            int sx = barX + i * stageW;
            g.fill(sx, barY - 2, sx + 1, barY + 10, 0xFFB8D6D6);
            g.fill(sx - 1, barY - 3, sx + 2, barY - 2, 0xFFB8D6D6);
        }

        // what to do next, above the bar
        Component hint = data.canEvolve()
                ? Component.translatable("celltostars.hud.evolve_ready", CellToStarsClient.mateKey.getTranslatedKeyMessage())
                : Component.translatable("celltostars.hud.editor_hint", CellToStarsClient.mateKey.getTranslatedKeyMessage());
        Component size = Component.translatable("celltostars.hud.stage", data.level(), stages);
        g.drawString(font, size, barX, top + 2, 0xFFB8D6D6);
        g.drawString(font, hint, barX + font.width(size) + 10, top + 2, data.canEvolve() ? 0xFFFFCC33 : 0xFF8FA8A8);
    }

    /**
     * Spore's corner panel: health bar on top, your cell in a frame below it (slowly turning).
     * Returns the panel's left edge.
     */
    private static int renderPortrait(GuiGraphics g, Font font, Minecraft mc, int w, int stripTop) {
        int box = 46;
        int panelW = box + 8;
        int x0 = w - panelW - 6;
        int y1 = stripTop + 22;           // reaches down into the strip
        int y0 = y1 - box - 22;           // room for the health bar above the portrait
        g.fill(x0 - 1, y0 - 1, x0 + panelW + 1, y1 + 1, 0xFF2E6B6B);
        g.fill(x0, y0, x0 + panelW, y1, 0xF0061218);

        // health bar with a little "+"
        float health = mc.player.getHealth();
        float max = Math.max(1, mc.player.getMaxHealth());
        int hpX = x0 + 11;
        int hpY = y0 + 5;
        int hpW = panelW - 15;
        g.fill(x0 + 3, hpY + 2, x0 + 8, hpY + 3, 0xFFFFFFFF);
        g.fill(x0 + 5, hpY, x0 + 6, hpY + 5, 0xFFFFFFFF);
        g.fill(hpX - 1, hpY - 1, hpX + hpW + 1, hpY + 6, 0xFF000000);
        g.fill(hpX, hpY, hpX + hpW, hpY + 5, 0xFF143A14);
        float share = Math.min(1, health / max);
        int color = share > 0.5f ? 0xFF3FDC3F : share > 0.25f ? 0xFFE0C030 : 0xFFE03030;
        g.fill(hpX, hpY, hpX + (int) (hpW * share), hpY + 5, color);
        g.fill(hpX, hpY, hpX + (int) (hpW * share), hpY + 1, 0x80FFFFFF);
        String hp = (int) Math.ceil(health) + "/" + (int) max;
        g.drawString(font, hp, x0 + panelW / 2 - font.width(hp) / 2, hpY + 7, 0xFFB8D6D6);

        // the portrait
        int px0 = x0 + 4;
        int py0 = y1 - box - 4;
        g.fill(px0 - 1, py0 - 1, px0 + box + 1, py0 + box + 1, 0xFF4E9A9A);
        g.fill(px0, py0, px0 + box, py0 + box, 0xFF0D2F48);
        g.fill(px0, py0, px0 + box, py0 + box / 2, 0xFF123C5A);
        g.enableScissor(px0, py0, px0 + box, py0 + box);
        float yaw = (Util.getMillis() % 12000L) / 12000f * 360f;
        CellEditorScreen.renderCellPreview(g, px0 + box / 2, py0 + box / 2, yaw, 35f, mc.player, box * 0.75f);
        g.disableScissor();
        return x0;
    }

    /** The camera is right above the cell, so the cell is in the middle of the screen. */
    private static void renderPopups(GuiGraphics g, Font font) {
        long now = Util.getMillis();
        POPUPS.removeIf(p -> now - p.startMs() > POPUP_MS);
        int centerX = g.guiWidth() / 2;
        int centerY = g.guiHeight() / 2;
        int index = 0;
        for (Popup popup : POPUPS) {
            long age = now - popup.startMs();
            float t = age / (float) POPUP_MS;
            int alpha = Math.max(8, (int) (255 * (1 - t)));
            int y = centerY - 14 - (int) (t * 24) - index * 10;
            g.drawCenteredString(font, Component.translatable("celltostars.hud.dna_gain", popup.amount()),
                    centerX, y, (alpha << 24) | 0x55FFFF);
            index++;
        }
    }
}
