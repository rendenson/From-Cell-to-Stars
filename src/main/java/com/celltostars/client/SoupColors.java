package com.celltostars.client;

import net.minecraft.Util;

/**
 * Water / murk colour of every growth level. When you grow, the colour slowly blends into the next one,
 * so it feels like swimming into a new, deeper part of the soup.
 */
public final class SoupColors {
    private static final float[][] LEVELS = {
            {0.10f, 0.32f, 0.27f}, // 1: green-teal shallows
            {0.08f, 0.28f, 0.38f}, // 2: blue-teal
            {0.07f, 0.17f, 0.40f}, // 3: deep blue
            {0.19f, 0.11f, 0.35f}, // 4: violet
            {0.09f, 0.09f, 0.18f}, // 5: dark abyss
    };

    private static final float[] current = LEVELS[0].clone();
    private static long lastMs = 0;

    private SoupColors() {
    }

    /** The colour to use right now (moves a little towards the level's colour every frame). */
    public static float[] current(int level) {
        float[] target = LEVELS[Math.max(0, Math.min(LEVELS.length - 1, level - 1))];
        long now = Util.getMillis();
        float dt = lastMs == 0 ? 1.0f : Math.min(1.0f, (now - lastMs) / 3000.0f); // ~3 s to blend fully
        lastMs = now;
        for (int i = 0; i < 3; i++) {
            current[i] += (target[i] - current[i]) * dt;
        }
        return current;
    }
}
