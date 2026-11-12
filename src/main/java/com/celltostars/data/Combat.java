package com.celltostars.data;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Spore's cell-stage combat rules (numbers from the Spore cell stage reference):
 * every cell has the same health; how much a hit takes depends on the SIZE RELATION
 * (prey / peer / predator / epic) and on the PART that hits.
 * All damage here is a share of the victim's max health (1.0 = kills it).
 */
public final class Combat {
    /** Health of every cell, player and AI (Spore: 6 HP each). 12 = 6 hearts. */
    public static final float CELL_HEALTH = 12.0f;

    private Combat() {
    }

    /** Relation of the attacker to the victim by size tier (half levels): +1 tier = predator, +2 = epic. */
    public enum Relation {
        PREY,      // attacker is smaller
        PEER,      // same level
        PREDATOR,  // attacker is a level bigger
        EPIC;      // attacker is 2+ levels bigger

        public static Relation of(int attackerTier, int victimTier) {
            int gap = attackerTier - victimTier;
            return gap >= 2 ? EPIC : gap == 1 ? PREDATOR : gap == 0 ? PEER : PREY;
        }
    }

    /** What an AI cell's bite takes from a player (Spore: epic 100%, predator 50%, peer ~20%). */
    public static float aiHitShare(Relation attackerToPlayer) {
        return switch (attackerToPlayer) {
            case EPIC -> 1.0f;
            case PREDATOR -> 0.5f;
            case PEER -> 0.2f;
            case PREY -> 0.08f;
        };
    }

    /**
     * What the player's front part takes from an AI cell. {@code victimToPlayer}: how the VICTIM relates to the player
     * (PREDATOR = the victim is bigger). Table (vs predator / peer / prey, out of 6 HP):
     * spike 8/3/0, jaw 5/3/6, proboscis 1/3/6, filter mouth 0.
     */
    public static float playerHitShare(Diet mouth, int spikes, Relation victimToPlayer) {
        float spike = spikes <= 0 ? 0 : switch (victimToPlayer) {
            case PREDATOR, EPIC -> 8f / 6f;
            case PEER -> 3f / 6f;
            case PREY -> 0f; // Spore: a spike does nothing to small cells - bite them instead
        } * (1.0f + 0.25f * (spikes - 1)); // extra spikes help a bit
        float bite = switch (mouth) {
            case CARNIVORE -> switch (victimToPlayer) {
                case PREDATOR, EPIC -> 5f / 6f;
                case PEER -> 3f / 6f;
                case PREY -> 1.0f;
            };
            default -> 0f; // filter mouth can't hurt; the proboscis works by sucking (see proboscisShare)
        };
        return Math.min(1.0f, Math.max(spike, bite));
    }

    /** One sip of the proboscis (vs predator / peer / prey: 1 / 3 / 6 of 6 HP). */
    public static float proboscisShare(Relation victimToPlayer) {
        return switch (victimToPlayer) {
            case PREDATOR, EPIC -> 1f / 6f;
            case PEER -> 3f / 6f;
            case PREY -> 1.0f;
        };
    }

    // ---------------- where the front of a cell is ----------------

    /** What a cell has at the front for a head-on clash. */
    public enum Front { SPIKE, JAW, NONE }

    public static Front frontOf(Diet mouth, int spikes) {
        if (spikes > 0) return Front.SPIKE;
        return mouth == Diet.CARNIVORE ? Front.JAW : Front.NONE;
    }

    /**
     * Head on, does MY attack get blocked? Spore: spikes colliding - neither hurts; spike against jaws -
     * the spike hurts, the jaws don't; jaws against jaws - they clack. Filters and the proboscis block nothing.
     */
    public static boolean blocked(Front mine, Front theirs) {
        if (mine == Front.NONE) return false;
        if (theirs == Front.SPIKE) return true;            // a spike wall stops spikes and jaws alike
        return mine == Front.JAW && theirs == Front.JAW;   // jaw vs jaw
    }

    /**
     * Are the two cells face to face - each one in front of the other's nose? Two jaws meeting like that
     * clack together and bounce off (Spore): to bite another meat eater you have to get it from the side.
     */
    public static boolean headOn(LivingEntity a, LivingEntity b) {
        float ya = a.getYRot() * Mth.DEG_TO_RAD;
        float yb = b.getYRot() * Mth.DEG_TO_RAD;
        double ax = -Mth.sin(ya), az = Mth.cos(ya);
        double bx = -Mth.sin(yb), bz = Mth.cos(yb);
        double dx = b.getX() - a.getX(), dz = b.getZ() - a.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0e-4) return false;
        dx /= len;
        dz /= len;
        boolean facing = ax * bx + az * bz < -0.5;         // looking at each other (within ~60 degrees)
        boolean bInFront = ax * dx + az * dz > 0.6;        // b is in front of a's mouth
        boolean aInFront = -(bx * dx + bz * dz) > 0.6;     // and a in front of b's mouth
        return facing && bInFront && aInFront;
    }

    /** The tip of the mouth / spikes, in front of the body (scaled with the cell). */
    public static Vec3 frontPoint(LivingEntity cell, double reachPerScale) {
        float yaw = cell.getYRot() * Mth.DEG_TO_RAD;
        double reach = reachPerScale * cell.getScale();
        return new Vec3(cell.getX() - Mth.sin(yaw) * reach, cell.getY() + cell.getBbHeight() * 0.3,
                cell.getZ() + Mth.cos(yaw) * reach);
    }

    /**
     * Does the attacker's front part reach the target? The part is a thick "stick" from the front of the body
     * to its tip (reachPerScale), {@code thickness} wide (per unit of scale); the target counts as a circle as wide
     * as its body. Generous on purpose: in Spore anything the mouth brushes gets bitten.
     */
    public static boolean reaches(LivingEntity attacker, double reachPerScale, double thickness, LivingEntity target) {
        double scale = attacker.getScale();
        if (Math.abs(target.getY() - attacker.getY()) > 3 * scale + target.getBbHeight()) return false; // other layer
        float yaw = attacker.getYRot() * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw);
        double fz = Mth.cos(yaw);
        double dx = target.getX() - attacker.getX();
        double dz = target.getZ() - attacker.getZ();
        double along = dx * fx + dz * fz;              // how far in front of me the target's centre is
        if (along <= 0) return false;                   // behind or beside me: never
        double side = Math.abs(dx * fz - dz * fx);      // how far off my nose line it is
        double targetRadius = Math.max(target.getBbWidth() * 0.7, 0.45 * target.getScale()); // its whole body, not just the middle
        double half = thickness * scale + targetRadius;
        double start = 0.25 * scale;                    // the front of my body
        double tip = (reachPerScale + 0.15) * scale + targetRadius;
        return side < half && along > start - targetRadius && along < tip;
    }

    /** Is the point touching the target's body (seen from above: only X/Z count)? */
    public static boolean touches(Vec3 point, LivingEntity target, double radius) {
        AABB box = target.getBoundingBox();
        // same layer only: cells far above/below (the giants in the background) can't be reached
        double slack = Math.max(radius * 3, 0.25);
        if (point.y < box.minY - slack || point.y > box.maxY + slack) return false;
        return point.x > box.minX - radius && point.x < box.maxX + radius
                && point.z > box.minZ - radius && point.z < box.maxZ + radius;
    }
}
