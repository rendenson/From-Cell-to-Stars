package com.celltostars.client;

import com.celltostars.data.EvoData;
import com.celltostars.data.Stage;

/** The client's copy of the local player's Spore state (the server sends it). */
public final class ClientEvoData {
    private static EvoData data = EvoData.EMPTY;

    /** Inside the egg = the editor must be on screen. */
    private static boolean inEgg = false;
    private static int editorMode = 0;

    /** Start times (ms) of the short "cutscenes"; 0 = not playing. */
    private static long loveStartMs = 0;
    private static long hatchStartMs = 0;

    private ClientEvoData() {
    }

    public static EvoData get() {
        return data;
    }

    public static void set(EvoData newData) {
        data = newData;
    }

    public static boolean isCell() {
        return data.stage() == Stage.CELL;
    }

    public static boolean isInEgg() {
        return inEgg;
    }

    public static int editorMode() {
        return editorMode;
    }

    public static void enterEgg(int mode) {
        inEgg = true;
        editorMode = mode;
        loveStartMs = 0;
    }

    public static void leaveEgg(long nowMs) {
        inEgg = false;
        hatchStartMs = nowMs;
    }

    public static long loveStartMs() {
        return loveStartMs;
    }

    public static void startLove(long nowMs) {
        loveStartMs = nowMs;
    }

    public static long hatchStartMs() {
        return hatchStartMs;
    }

    /** True for the ~2 seconds the egg / meteorite is on screen after pressing "Hatch". */
    public static boolean isHatching() {
        return hatchStartMs != 0 && net.minecraft.Util.getMillis() - hatchStartMs < 1800;
    }

    /** The egg scene without the editor (after respawning). */
    public static void startHatchScene(long nowMs) {
        if (!inEgg) hatchStartMs = nowMs;
    }

    /** Leaving the egg without hatching (evolving): no egg scene. */
    public static void leaveEggNoScene() {
        inEgg = false;
    }

    /** True while a movement key is held in the cell stage. */
    private static boolean swimming = false;

    public static boolean isSwimming() {
        return swimming;
    }

    public static void setSwimming(boolean value) {
        swimming = value;
    }

    public static void reset() {
        data = EvoData.EMPTY;
        inEgg = false;
        loveStartMs = 0;
        hatchStartMs = 0;
    }
}
