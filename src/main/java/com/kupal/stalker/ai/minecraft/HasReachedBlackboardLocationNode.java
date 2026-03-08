package com.kupal.stalker.ai.minecraft;

import com.kupal.stalker.ai.*;
import org.bukkit.Location;
import org.bukkit.entity.Mob;

public final class HasReachedBlackboardLocationNode implements BtNode {
    private final BlackboardKey<Location> key;
    private final double distance;

    public HasReachedBlackboardLocationNode(BlackboardKey<Location> key, double distance) {
        this.key = key;
        this.distance = distance;
    }

    @Override
    public Status tick(BtContext ctx, Mob mob, Blackboard bb) {
        Location target = bb.get(key).orElse(null);
        if (target == null || target.getWorld() == null) {
            return Status.FAILURE;
        }
        if (!target.getWorld().equals(mob.getWorld())) {
            return Status.FAILURE;
        }

        double distSq = mob.getLocation().distanceSquared(target);
        return distSq <= distance * distance ? Status.SUCCESS : Status.FAILURE;
    }
}