package com.kupal.stalker.stalker.tree;

import com.kupal.stalker.ai.*;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public final class TargetDistanceLessThanNode implements BtNode {

    private final double distance;

    public TargetDistanceLessThanNode(double distance) {
        this.distance = distance;
    }

    @Override
    public Status tick(BtContext ctx, Mob npc, Blackboard bb) {
        Player target = bb.get(StalkerBbKeys.TARGET_PLAYER).orElse(null);
        if (target == null) return Status.FAILURE;

        boolean distanceLess = target.getLocation().distanceSquared(npc.getLocation()) < distance * distance;

        if  (distanceLess) {;
            bb.put(StalkerBbKeys.DEBUG_REASON, "target closer than " + distance);
        }

        return distanceLess ? Status.SUCCESS : Status.FAILURE;
    }
}