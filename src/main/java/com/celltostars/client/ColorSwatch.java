package com.celltostars.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** A small clickable colour square for the paint section of the editor. */
public class ColorSwatch extends AbstractButton {
    private final int color;
    private final boolean selected;
    private final Runnable onPick;

    public ColorSwatch(int x, int y, int size, int color, boolean selected, Runnable onPick) {
        super(x, y, size, size, Component.empty());
        this.color = color;
        this.selected = selected;
        this.onPick = onPick;
    }

    @Override
    public void onPress() {
        onPick.run();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int border = selected ? 0xFFFFFFFF : isHovered() ? 0xFFAAAAAA : 0xFF000000;
        g.fill(getX(), getY(), getX() + width, getY() + height, border);
        g.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, 0xFF000000 | color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
