package com.celltostars.client;

import com.mojang.blaze3d.platform.Window;
import com.celltostars.CellToStars;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * "Window mode" option in Options -> Video Settings: windowed / borderless / fullscreen.
 * Borderless = a window without frame covering the whole screen: looks like fullscreen, but Alt+Tab is instant.
 * The choice is saved in config/celltostars.properties and applied every time the game starts.
 */
public enum WindowMode {
    WINDOWED("windowed"),
    BORDERLESS("borderless"),
    FULLSCREEN("fullscreen");

    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("celltostars.properties");
    private static WindowMode current = WINDOWED;

    private final String id;

    WindowMode(String id) {
        this.id = id;
    }

    private WindowMode next() {
        WindowMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    private Component label() {
        return Component.translatable("celltostars.options.window",
                Component.translatable("celltostars.options.window." + id));
    }

    /** Called once from the client initializer. */
    public static void init() {
        load();
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED
                .register(client -> current.apply(client));

        // a button in the top-right corner of Video Settings
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof VideoSettingsScreen)) return;
            // the screen fires this again after every resize (switching the mode resizes!) without clearing: no doubles
            Screens.getButtons(screen).removeIf(w -> w instanceof WindowButton);
            // bottom bar, left of "Done" - the top bar is taken by the title on small screens
            int x = Math.max(4, width / 2 - 100 - 8 - 150);
            Button button = new WindowButton(x, height - 27, 150, 20, current.label(), b -> {
                current = current.next();
                current.apply(client);
                save();
                b.setMessage(current.label());
            });
            button.setTooltip(Tooltip.create(Component.translatable("celltostars.options.window.desc")));
            Screens.getButtons(screen).add(button);
        });
    }

    /** Our button, so we can find it again. */
    private static final class WindowButton extends Button {
        WindowButton(int x, int y, int w, int h, Component message, OnPress onPress) {
            super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
        }
    }

    private void apply(Minecraft client) {
        Window window = client.getWindow();
        long handle = window.getWindow();

        // leave exclusive fullscreen first, unless that is what we want
        if (this != FULLSCREEN && window.isFullscreen()) {
            window.toggleFullScreen();
            client.options.fullscreen().set(false);
        }

        switch (this) {
            case WINDOWED -> {
                GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
                GLFW.glfwMaximizeWindow(handle);
            }
            case BORDERLESS -> {
                long monitor = GLFW.glfwGetPrimaryMonitor();
                GLFWVidMode mode = monitor == 0 ? null : GLFW.glfwGetVideoMode(monitor);
                if (mode == null) return;
                int[] mx = new int[1];
                int[] my = new int[1];
                GLFW.glfwGetMonitorPos(monitor, mx, my);
                GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
                // 1 px taller than the screen: a frameless window that exactly matches the monitor is treated by
                // Windows/drivers as real (exclusive) fullscreen -> black screen on Alt+Tab. The extra pixel avoids that.
                GLFW.glfwSetWindowMonitor(handle, 0, mx[0], my[0], mode.width(), mode.height() + 1, GLFW.GLFW_DONT_CARE);
            }
            case FULLSCREEN -> {
                GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
                if (!window.isFullscreen()) {
                    window.toggleFullScreen();
                    client.options.fullscreen().set(true);
                }
            }
        }
    }

    private static void load() {
        if (!Files.exists(CONFIG)) return;
        try (Reader reader = Files.newBufferedReader(CONFIG)) {
            Properties props = new Properties();
            props.load(reader);
            String saved = props.getProperty("windowMode", WINDOWED.id);
            for (WindowMode m : values()) {
                if (m.id.equals(saved)) current = m;
            }
        } catch (IOException e) {
            CellToStars.LOGGER.warn("Could not read {}", CONFIG, e);
        }
    }

    private static void save() {
        Properties props = new Properties();
        props.setProperty("windowMode", current.id);
        try (Writer writer = Files.newBufferedWriter(CONFIG)) {
            props.store(writer, "Spore mod settings");
        } catch (IOException e) {
            CellToStars.LOGGER.warn("Could not save {}", CONFIG, e);
        }
    }
}
