package com.celltostars.client;

import com.celltostars.entity.CellCreature;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Cells bigger than you are solid: you can't swim through them, you slide around them.
 * Done on the client because the client moves the player; the server just accepts the position.
 * (Cells your size or smaller stay "soft", so biting and eating them isn't blocked.)
 */
public final class CellCollisions {
    private CellCollisions() {
    }

    public static void push(LocalPlayer player) {
        int myTier = ClientEvoData.get().tier();
        double myRadius = player.getBbWidth() * 0.5;
        for (CellCreature cell : player.level().getEntitiesOfClass(CellCreature.class,
                player.getBoundingBox().inflate(8 * player.getScale()), CellCreature::isAlive)) {
            if (cell.cellTier() <= myTier) continue;                                         // not bigger than me
            if (Math.abs(cell.getY() - player.getY()) > cell.getBbHeight() + player.getBbHeight()) continue; // deep below
            // its body as a circle (long or wide species count with their bigger side)
            var species = cell.species();
            double radius = cell.getBbWidth() * 0.5 * Math.max(species.bodyWidth, species.bodyLength);
            double dx = player.getX() - cell.getX();
            double dz = player.getZ() - cell.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            double min = radius + myRadius;
            if (dist >= min) continue;
            if (dist < 1.0e-4) {
                dx = 1;
                dz = 0;
                dist = 1;
            }
            double nx = dx / dist;
            double nz = dz / dist;
            // put me back on its edge and drop the part of my speed that goes into it (so I slide along)
            player.setPos(cell.getX() + nx * min, player.getY(), cell.getZ() + nz * min);
            Vec3 v = player.getDeltaMovement();
            double into = v.x * nx + v.z * nz;
            if (into < 0) player.setDeltaMovement(v.x - into * nx, v.y, v.z - into * nz);
        }
    }
}
