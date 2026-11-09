package com.celltostars.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.celltostars.CellToStars;
import com.celltostars.data.EvoData;
import com.celltostars.entity.EvoEntities;
import com.celltostars.network.EvoNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.Util;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import com.celltostars.data.CellSpeed;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class CellToStarsClient implements ClientModInitializer {
    public static KeyMapping mateKey;

    @Override
    public void onInitializeClient() {
        // food particles are drawn as a flat item sprite (kelp / beef), the mate as a player head
        EntityRendererRegistry.register(EvoEntities.FOOD_PARTICLE, FoodRenderer::new);
        EntityRendererRegistry.register(EvoEntities.MATE, MateRenderer::new);
        EntityRendererRegistry.register(EvoEntities.HATCH_SHELL, HatchShellRenderer::new);
        EntityRendererRegistry.register(EvoEntities.CELL_CREATURE, CellCreatureRenderer::new);
        EntityRendererRegistry.register(EvoEntities.PLANT_BUSH, PlantBushRenderer::new);

        // little health bars over hurt creatures
        WorldRenderEvents.AFTER_ENTITIES.register(HealthBars::render);

        ClientPlayNetworking.registerGlobalReceiver(EvoNetwork.SyncPayload.TYPE, (payload, context) -> {
            EvoData old = ClientEvoData.get();
            EvoData now = payload.data();
            ClientEvoData.set(now);
            if (now.stage() != com.celltostars.data.Stage.CELL) ClientEvoData.leaveEggNoScene();
            if (old.stage() == now.stage() && now.dna() > old.dna() && !ClientEvoData.isInEgg()) {
                EvoHud.onDnaGained(now.dna() - old.dna());
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(EvoNetwork.OpenEditorPayload.TYPE,
                (payload, context) -> ClientEvoData.enterEgg(payload.mode()));
        ClientPlayNetworking.registerGlobalReceiver(EvoNetwork.HatchScenePayload.TYPE,
                (payload, context) -> ClientEvoData.startHatchScene(Util.getMillis()));
        ClientPlayNetworking.registerGlobalReceiver(EvoNetwork.LovePayload.TYPE,
                (payload, context) -> ClientEvoData.startLove(Util.getMillis()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientEvoData.reset());

        mateKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.celltostars.mate", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.celltostars"));

        ClientTickEvents.END_CLIENT_TICK.register(CellToStarsClient::onClientTick);

        // "Window mode" button in Video Settings (windowed / borderless / fullscreen)
        WindowMode.init();

        HudRenderCallback.EVENT.register(EvoHud::render);
    }

    /**
     * Cell swimming speed comes only from the speed rule in CellSpeed (flagella = speed points, scaled by size),
     * the same rule the AI cells use. We steer the velocity towards that speed instead of Minecraft's water physics.
     */
    private static double swimSpeed = 0;

    private static void applySwimSpeed(LocalPlayer player) {
        boolean moving = ClientEvoData.isSwimming() && !ClientEvoData.isInEgg();
        double k = player.getScale() / 0.3;
        int points = ClientEvoData.get().count(com.celltostars.data.Part.FLAGELLUM);
        double target = moving ? CellSpeed.blocksPerTick(points, k) : 0;
        swimSpeed += (target - swimSpeed) * (moving ? 0.3 : 0.15); // smooth speed-up and glide to a stop
        float yawRad = player.getYRot() * Mth.DEG_TO_RAD;
        // the next tick moves the cell by exactly this much (vanilla water push is switched off in KeyboardInputMixin)
        player.setDeltaMovement(-Mth.sin(yawRad) * swimSpeed, 0, Mth.cos(yawRad) * swimSpeed);
    }

    private static void onClientTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) return;
        boolean cell = ClientEvoData.isCell();

        // while in the egg the editor is always on screen (also after the pause menu or a world change closed it)
        if (ClientEvoData.isInEgg() && client.screen == null) {
            client.setScreen(new CellEditorScreen());
        }

        while (mateKey.consumeClick()) {
            if (cell && !ClientEvoData.isInEgg()) {
                ClientPlayNetworking.send(EvoNetwork.CallMatePayload.INSTANCE);
            }
        }
        if (!cell) return;

        // the cell is seen from outside, never first person
        if (client.options.getCameraType().isFirstPerson()) {
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }

        // cells live on one flat layer: no swimming up or down
        if (client.level != null && client.level.dimension() == CellToStars.CELL_LEVEL) {
            if (Math.abs(player.getY() - CellToStars.CELL_PLANE_Y) > 1.0e-3) {
                player.setPos(player.getX(), CellToStars.CELL_PLANE_Y, player.getZ());
            }
            player.setSprinting(false);
            applySwimSpeed(player);
        }
    }
}
