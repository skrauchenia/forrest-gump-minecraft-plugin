package com.kupal.stalker.ai.minecraft;

import com.kupal.stalker.ai.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class FindCoverNode implements BtNode {
    private final int horizontalRadius;
    private final int verticalRadius;
    private final int samples;

    public FindCoverNode(int horizontalRadius, int verticalRadius, int samples) {
        this.horizontalRadius = horizontalRadius;
        this.verticalRadius = verticalRadius;
        this.samples = samples;
    }

    @Override
    public Status tick(BtContext ctx, Mob mob, Blackboard bb) {
        Player threat = bb.get(BbKeys.THREAT_PLAYER).orElse(null);
        if (threat == null || !threat.isOnline() || threat.isDead()) {
            bb.remove(BbKeys.COVER_LOCATION);
            return Status.FAILURE;
        }

        Location mobLoc = mob.getLocation();
        Location playerEye = threat.getEyeLocation();
        World world = mob.getWorld();

        double currentDistSq = mobLoc.distanceSquared(threat.getLocation());
        Location best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < samples; i++) {
            int dx = randomInt(-horizontalRadius, horizontalRadius);
            int dz = randomInt(-horizontalRadius, horizontalRadius);
            int dy = randomInt(-verticalRadius, verticalRadius);

            Location candidate = mobLoc.clone().add(dx, dy, dz);
            Location grounded = findNearestStandable(world, candidate, verticalRadius);
            if (grounded == null) continue;

            double candidateDistSq = grounded.distanceSquared(threat.getLocation());
            if (candidateDistSq <= currentDistSq) continue;

            if (hasLineOfSight(playerEye, grounded, world)) continue;

            double score = candidateDistSq;
            if (score > bestScore) {
                bestScore = score;
                best = grounded;
            }
        }

        if (best == null) {
            bb.remove(BbKeys.COVER_LOCATION);
            return Status.FAILURE;
        }

        bb.put(BbKeys.COVER_LOCATION, best);
        bb.put(BbKeys.MOVE_TARGET, best);
        return Status.SUCCESS;
    }

    private static int randomInt(int min, int max) {
        return min + (int) (Math.random() * (max - min + 1));
    }

    private static Location findNearestStandable(World world, Location around, int maxYAdjust) {
        int baseX = around.getBlockX();
        int baseY = around.getBlockY();
        int baseZ = around.getBlockZ();

        for (int offset = 0; offset <= maxYAdjust; offset++) {
            Location down = tryStandable(world, baseX, baseY - offset, baseZ);
            if (down != null) return down;

            if (offset != 0) {
                Location up = tryStandable(world, baseX, baseY + offset, baseZ);
                if (up != null) return up;
            }
        }
        return null;
    }

    private static Location tryStandable(World world, int x, int y, int z) {
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Block ground = world.getBlockAt(x, y - 1, z);

        if (!feet.isPassable()) return null;
        if (!head.isPassable()) return null;
        if (ground.isPassable() || ground.getType() == Material.AIR) return null;

        return new Location(world, x + 0.5, y, z + 0.5);
    }

    private static boolean hasLineOfSight(Location from, Location to, World world) {
        Vector dir = to.toVector().subtract(from.toVector());
        double length = dir.length();
        if (length == 0) return true;

        dir.normalize().multiply(0.3);
        Location probe = from.clone();
        double traveled = 0.0;

        while (traveled < length) {
            Block block = world.getBlockAt(probe);
            if (!block.isPassable()) return false;
            probe.add(dir);
            traveled += 0.3;
        }

        return true;
    }
}