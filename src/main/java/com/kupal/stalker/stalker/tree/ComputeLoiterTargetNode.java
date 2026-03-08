package com.kupal.stalker.stalker.tree;

import com.kupal.stalker.ai.*;
import com.kupal.stalker.services.MovementService;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public final class ComputeLoiterTargetNode implements BtNode {

    private final MovementService movement;
    private final double loiterDistance;

    public ComputeLoiterTargetNode(MovementService movement, double loiterDistance) {
        this.movement = movement;
        this.loiterDistance = loiterDistance;
    }

    @Override
    public Status tick(BtContext ctx, Mob npc, Blackboard bb) {
        Player target = bb.get(StalkerBbKeys.TARGET_PLAYER).orElse(null);
        if (target == null) return Status.FAILURE;

        Location p = movement.computeLoiterPoint(npc, target, loiterDistance);
        if (p == null) return Status.FAILURE;

        bb.put(StalkerBbKeys.LOITER_TARGET, p);
        bb.put(StalkerBbKeys.MOVE_TARGET, p);
        return Status.SUCCESS;
    }
}