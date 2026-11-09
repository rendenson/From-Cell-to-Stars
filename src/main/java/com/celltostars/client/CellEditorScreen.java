package com.celltostars.client;

import com.celltostars.data.Part;
import com.celltostars.data.EvoData;
import com.celltostars.network.EvoNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Full-screen cell editor, like in Spore. You are inside your egg while it is open.
 * Left: parts ([-] sells one back, the big button buys one more). Middle: your cell. Bottom: name + hatch.
 */
public class CellEditorScreen extends Screen {
    private static final int PREVIEW_LEFT = 210;

    private EvoData shown;
    // the preview stays still until you drag it with the left mouse button
    private float previewYaw = 180.0f;
    private float previewTilt = 55.0f;
    private EditBox nameBox;
    private Button hatchButton;
    private int paintTop;
    private Button evolveButton;
    private String typedName;

    public CellEditorScreen() {
        super(Component.translatable("celltostars.editor.title"));
    }

    @Override
    protected void init() {
        shown = ClientEvoData.get();
        if (typedName == null) typedName = shown.name();

        // ---- left column: parts + diet ----
        int left = 20;
        int y = 60;
        for (Part part : Part.values()) {
            int count = shown.count(part);
            Component name = Component.translatable(part.nameKey());

            // [-] sells one back
            Button sell = Button.builder(Component.literal("-"),
                            b -> ClientPlayNetworking.send(new EvoNetwork.PartPayload(part.id(), false)))
                    .bounds(left, y, 20, 20)
                    .tooltip(Tooltip.create(Component.translatable("celltostars.editor.sell", part.cost())))
                    .build();
            sell.active = count > 0;
            addRenderableWidget(sell);

            // main button buys one more
            Button buy = Button.builder(
                            Component.translatable("celltostars.editor.part_row", name, count, part.maxCount(), part.cost()),
                            b -> ClientPlayNetworking.send(new EvoNetwork.PartPayload(part.id(), true)))
                    .bounds(left + 24, y, 146, 20)
                    .tooltip(Tooltip.create(Component.translatable(part.descKey())))
                    .build();
            buy.active = count < part.maxCount() && shown.dna() >= part.cost();
            addRenderableWidget(buy);
            y += 24;
        }

        y += 14;
        Component diet = Component.translatable(shown.diet().translationKey());
        addRenderableWidget(Button.builder(Component.translatable("celltostars.editor.diet", diet),
                        b -> ClientPlayNetworking.send(new EvoNetwork.SetDietPayload(shown.diet().next().getSerializedName())))
                .bounds(left, y, 170, 20)
                .tooltip(Tooltip.create(Component.translatable("celltostars.editor.diet.desc")))
                .build());

        // ---- paint: primary colour, secondary colour, pattern (free, like in Spore) ----
        y += 34;
        paintTop = y;
        y = addPalette(left, y + 10, shown.primary(), true);
        y = addPalette(left, y + 14, shown.secondary(), false);
        Component pattern = Component.translatable(com.celltostars.data.Pattern.byId(shown.pattern()).translationKey());
        addRenderableWidget(Button.builder(Component.translatable("celltostars.editor.pattern", pattern),
                        b -> ClientPlayNetworking.send(new EvoNetwork.SetLookPayload(shown.primary(), shown.secondary(),
                                com.celltostars.data.Pattern.byId(shown.pattern()).next().id())))
                .bounds(left, y + 6, 170, 20)
                .build());

        // ---- bottom middle: name + hatch ----
        int center = width / 2;
        nameBox = new EditBox(font, center - 80, height - 62, 160, 20, Component.translatable("celltostars.editor.name"));
        nameBox.setMaxLength(24);
        nameBox.setHint(Component.translatable("celltostars.editor.name_hint"));
        nameBox.setValue(typedName);
        nameBox.setResponder(text -> {
            typedName = text;
            updateHatchButtons();
        });
        addRenderableWidget(nameBox);

        if (shown.canEvolve()) {
            // bar is full: evolve now, or hatch once more to keep collecting parts
            hatchButton = addRenderableWidget(Button.builder(Component.translatable("celltostars.editor.hatch"), b -> hatch())
                    .bounds(center - 164, height - 36, 160, 20)
                    .build());
            evolveButton = addRenderableWidget(Button.builder(Component.translatable("celltostars.editor.evolve"), b -> evolve())
                    .bounds(center + 4, height - 36, 160, 20)
                    .tooltip(Tooltip.create(Component.translatable("celltostars.editor.evolve.desc")))
                    .build());
        } else {
            hatchButton = addRenderableWidget(Button.builder(Component.translatable("celltostars.editor.hatch"), b -> hatch())
                    .bounds(center - 80, height - 36, 160, 20)
                    .build());
        }
        updateHatchButtons();
    }

    private static final int[] PALETTE = {
            0xE8E8E8, 0xF2D35B, 0xF29B38, 0xD9483B, 0xE86FB0, 0x9A5CC9,
            0x4A63D9, 0x6FC3DF, 0x2E9E8E, 0x7FCF5A, 0x8B5A2B, 0x2B2B33,
    };

    /** 12 colour squares in two rows. Returns the y below them. */
    private int addPalette(int left, int y, int current, boolean primary) {
        int size = 26;
        for (int i = 0; i < PALETTE.length; i++) {
            int color = PALETTE[i];
            int x = left + (i % 6) * (size + 3);
            int rowY = y + (i / 6) * (size + 3);
            addRenderableWidget(new ColorSwatch(x, rowY, size, color, color == current, () ->
                    ClientPlayNetworking.send(new EvoNetwork.SetLookPayload(
                            primary ? color : shown.primary(),
                            primary ? shown.secondary() : color,
                            shown.pattern()))));
        }
        return y + 2 * (size + 3);
    }

    /** Your creature needs a name before it can hatch. */
    private void updateHatchButtons() {
        boolean named = typedName != null && !typedName.isBlank();
        Tooltip needName = named ? null : Tooltip.create(Component.translatable("celltostars.editor.need_name"));
        if (hatchButton != null) {
            hatchButton.active = named;
            hatchButton.setTooltip(needName);
        }
        if (evolveButton != null) {
            evolveButton.active = named;
        }
    }

    private void hatch() {
        if (typedName == null || typedName.isBlank()) return;
        ClientPlayNetworking.send(new EvoNetwork.HatchPayload(typedName == null ? "" : typedName));
        ClientEvoData.leaveEgg(Util.getMillis());
        onClose();
    }

    private void evolve() {
        if (typedName == null || typedName.isBlank()) return;
        ClientPlayNetworking.send(new EvoNetwork.EvolvePayload(typedName));
        ClientEvoData.leaveEggNoScene();
        onClose();
    }

    /**
     * Entering the editor: the soup goes quiet, like stepping into another room.
     * The editor music itself is chosen in MinecraftMusicMixin.
     */
    @Override
    public void added() {
        super.added();
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().pause();
        mc.getMusicManager().stopPlaying();
    }

    @Override
    public void removed() {
        super.removed();
        Minecraft mc = Minecraft.getInstance();
        mc.getMusicManager().stopPlaying(); // editor music ends, the soup music comes after a short pause
        mc.getSoundManager().resume();
    }

    @Override
    public void tick() {
        // server answered (part bought / sold, diet changed) -> redraw buttons
        if (ClientEvoData.get() != shown) {
            rebuildWidgets();
        }
    }

    /** Esc opens the normal pause menu (to quit the game); the editor comes back after it. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && !nameBox.isFocused()) { // 256 = Escape
            minecraft.setScreen(new PauseScreen(true));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // deep sea gradient instead of the blurred world
        g.fillGradient(0, 0, width, height, 0xFF0D3B3A, 0xFF041615);
        // side panel
        g.fill(10, 30, 200, height - 10, 0x66000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int center = width / 2;

        Component title = switch (ClientEvoData.editorMode()) {
            case EvoNetwork.EDITOR_CREATE -> Component.translatable("celltostars.editor.title_create");
            case EvoNetwork.EDITOR_RESUME -> Component.translatable("celltostars.editor.title_resume");
            default -> this.title;
        };
        g.drawCenteredString(font, title, center, 12, 0xFFFFFF);
        g.drawString(font, Component.translatable("celltostars.editor.parts"), 20, 44, 0xAAFFEE);
        g.drawString(font, Component.translatable("celltostars.editor.primary"), 20, paintTop, 0xAAFFEE);
        g.drawString(font, Component.translatable("celltostars.editor.secondary"), 20, paintTop + 72, 0xAAFFEE);
        g.drawString(font, Component.translatable("celltostars.hud.dna", shown.dna()), width - 140, 44, 0x55FFFF);
        DietPathBar.draw(g, font, width - 140, 60, 120, shown);

        g.drawCenteredString(font, Component.translatable("celltostars.editor.rotate_hint"),
                (PREVIEW_LEFT + width) / 2, height - 82, 0x557777);

        // the cell itself in the middle; hold the left mouse button and drag to turn it
        if (minecraft.player != null) {
            int cx = (PREVIEW_LEFT + width - 10) / 2;
            int cy = (30 + height - 90) / 2;
            renderCellPreview(g, cx, cy, previewYaw, previewTilt, minecraft.player, 150);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && mouseX > PREVIEW_LEFT && mouseY < height - 90) {
            previewYaw += (float) dragX * 1.5f;
            previewTilt = Mth.clamp(previewTilt - (float) dragY * 1.5f, -90f, 90f);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /** Draws the cell turned by yaw/tilt around its middle; {@code lengthPx} = how long the body is on screen. */
    static void renderCellPreview(GuiGraphics g, int cx, int cy, float yaw, float tiltDegrees, LivingEntity cell, float lengthPx) {
        // the cell stays put (facing yaw 180); we turn the "camera" around it instead
        Quaternionf camera = new Quaternionf()
                .rotateX(tiltDegrees * Mth.DEG_TO_RAD)
                .rotateY((yaw - 180.0f) * Mth.DEG_TO_RAD);
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI).mul(camera);

        float s = cell.getScale();
        int pixelsPerBlock = Math.max(1, (int) (lengthPx * 1.5f / s));
        // turn around the middle of the cell body
        Vector3f headCentre = new Vector3f(0, CellRenderer.CENTRE_HEIGHT * CellRenderer.SIZE_PER_SCALE * s, 0);
        pose.transform(headCentre);
        Vector3f offset = headCentre.negate();

        float oldBody = cell.yBodyRot;
        float oldBodyO = cell.yBodyRotO;
        float oldYaw = cell.getYRot();
        float oldPitch = cell.getXRot();
        float oldHead = cell.yHeadRot;
        float oldHeadO = cell.yHeadRotO;
        cell.yBodyRot = 180.0f;
        cell.yBodyRotO = 180.0f;
        cell.setYRot(180.0f);
        cell.setXRot(0);
        cell.yHeadRot = 180.0f;
        cell.yHeadRotO = 180.0f;

        g.pose().pushPose();
        g.pose().translate(0, 0, 300); // in front of the background, so nothing gets cut off
        InventoryScreen.renderEntityInInventory(g, cx, cy, pixelsPerBlock, offset, pose, camera, cell);
        g.pose().popPose();

        cell.yBodyRot = oldBody;
        cell.yBodyRotO = oldBodyO;
        cell.setYRot(oldYaw);
        cell.setXRot(oldPitch);
        cell.yHeadRot = oldHead;
        cell.yHeadRotO = oldHeadO;
    }

    @Override
    public boolean isPauseScreen() {
        return false; // the cell is safe inside the egg anyway (can't be hurt, doesn't move)
    }
}
