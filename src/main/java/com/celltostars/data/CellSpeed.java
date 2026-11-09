package com.celltostars.data;

/**
 * One speed rule for everybody (player and AI cells), like Spore:
 * speed comes from "speed points" given by movement parts (each flagellum = 1 point, max 5).
 * Size does not make you faster or slower by itself: when everything grows, the camera zooms out,
 * so speed is measured in "cell sizes" - a twice as big cell covers twice as many blocks.
 */
public final class CellSpeed {
    public static final int MAX_POINTS = 5;
    private static final double BASE = 0.07;       // blocks per tick for a level-1 cell with no parts (~1.4 blocks/s)
    private static final double PER_POINT = 0.012; // each point adds ~17%

    private CellSpeed() {
    }

    /** @param sizeFactor how big compared to a level-1 cell (1.0 .. 2.6) */
    public static double blocksPerTick(int speedPoints, double sizeFactor) {
        return (BASE + PER_POINT * Math.min(MAX_POINTS, Math.max(0, speedPoints))) * sizeFactor;
    }
}
