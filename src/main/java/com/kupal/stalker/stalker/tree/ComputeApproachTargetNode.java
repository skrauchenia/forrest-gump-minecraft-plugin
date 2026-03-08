package com.kupal.stalker.stalker.tree;

import com.kupal.stalker.ai.*;
import com.kupal.stalker.services.MovementService;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public final class ComputeApproachTargetNode implements BtNode {

    private final MovementService movement;
    private final double behindDistance;

    public ComputeApproachTargetNode(MovementService movement, double behindDistance) {
        this.movement = movement;
        this.behindDistance = behindDistance;
    }

    @Override
    public Status tick(BtContext ctx, Mob npc, Blackboard bb) {
        Player target = bb.get(StalkerBbKeys.TARGET_PLAYER).orElse(null);
        if (target == null) return Status.FAILURE;

        Location p = movement.computeApproachPointBehindTarget(npc, target, behindDistance);
        if (p == null) return Status.FAILURE;

        bb.put(StalkerBbKeys.APPROACH_TARGET, p);
        bb.put(StalkerBbKeys.MOVE_TARGET, p);
        return Status.SUCCESS;
    }
}