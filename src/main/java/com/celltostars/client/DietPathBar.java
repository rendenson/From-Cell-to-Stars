package com.celltostars.client;

import com.celltostars.data.EvoData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Spore's "what are you becoming" bar: green (plants) on the left, red (meat) on the right,
 * and a marker that moves with what you have eaten so far.
 */
public final class DietPathBar {
    private DietPathBar() {
    }

    /** Draws the bar at (x, y); returns the y below it. */
    public static int draw(GuiGraphics g, Font font, int x, int y, int width, EvoData data) {
        String path = data.dietPath();
        g.drawString(font, Component.translatable("celltostars.hud.path", Component.translatable("celltostars.path." + path)),
                x, y, 0xFFDDDDDD);
        int barY = y + 11;
        g.fill(x - 1, barY - 1, x + width + 1, barY + 5, 0xFF000000);
        g.fillGradient(x, barY, x + width, barY + 4, 0xFF4FC24F, 0xFF4FC24F); // left part
        int third = width / 4;
        g.fill(x, barY, x + third, barY + 4, 0xFF4FC24F);                      // herbivore zone (< 25% meat)
        g.fill(x + third, barY, x + width - third, barY + 4, 0xFFD9B23B);      // omnivore zone
        g.fill(x + width - third, barY, x + width, barY + 4, 0xFFD9483B);      // carnivore zone
        int marker = x + Math.round(data.meatShare() * width);
        g.fill(marker - 1, barY - 3, marker + 2, barY + 7, 0xFFFFFFFF);
        return barY + 10;
    }
}
