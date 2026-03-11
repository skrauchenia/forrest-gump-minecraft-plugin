package com.kupal.stalker.stalker.tree;

import com.kupal.stalker.ai.*;
import com.kupal.stalker.services.MovementService;
import com.kupal.stalker.util.ToString;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public final class ComputeRetreatTargetNode implements BtNode {

    private final MovementService movement;
    private final double retreatDistance;
    private final Logger log = Logger.getLogger(ComputeRetreatTargetNode.class.getSimpleName());

    public ComputeRetreatTargetNode(MovementService movement, double retreatDistance) {
        this.movement = movement;
        this.retreatDistance = retreatDistance;
    }

    @Override
    public Status tick(BtContext ctx, Mob npc, Blackboard bb) {
        Player target = bb.get(StalkerBbKeys.TARGET_PLAYER).orElse(null);
        if (target == null) return Status.FAILURE;

        Location p = movement.computeRetreatPoint(npc, target, 10);

        bb.put(StalkerBbKeys.RETREAT_TARGET, p);
        bb.put(StalkerBbKeys.MOVE_TARGET, p);
        log.info("Chosen retreat point to %s at %s".formatted(target.getName(), ToString.of(p)));
        return Status.SUCCESS;
    }
}