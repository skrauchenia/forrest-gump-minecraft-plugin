package com.kupal.stalker.stalker.tree;

import com.kupal.stalker.ai.*;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public final class TargetDistanceGreaterThanNode implements BtNode {

    private final double distance;

    public TargetDistanceGreaterThanNode(double distance) {
        this.distance = distance;
    }

    @Override
    public Status tick(BtContext ctx, Mob npc, Blackboard bb) {
        Player target = bb.get(StalkerBbKeys.TARGET_PLAYER).orElse(null);
        if (target == null) return Status.FAILURE;

        boolean distanceGreater = target.getLocation().distanceSquared(npc.getLocation()) > distance * distance;

        if  (distanceGreater) {
            bb.put(StalkerBbKeys.DEBUG_REASON, "target farther than " + distance);
        }

        return distanceGreater ? Status.SUCCESS : Status.FAILURE;
    }
}