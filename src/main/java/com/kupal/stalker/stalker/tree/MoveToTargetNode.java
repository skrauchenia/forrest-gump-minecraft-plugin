package com.kupal.stalker.stalker.tree;

import com.kupal.stalker.ai.*;
import com.kupal.stalker.ai.nodes.StatefulActionNode;
import com.kupal.stalker.util.ToString;
import org.bukkit.Location;
import org.bukkit.entity.Mob;

import java.util.logging.Logger;

public final class MoveToTargetNode extends StatefulActionNode {

    private final BlackboardKey<Location> targetKey;
    private final double stopDistance;
    private final double speed;
    private final Logger log = Logger.getLogger("MoveToTargetNode");

    public MoveToTargetNode(BlackboardKey<Location> targetKey, double stopDistance, double speed) {
        this.targetKey = targetKey;
        this.stopDistance = stopDistance;
        this.speed = speed;
    }

    @Override
    protected Status onTick(BtContext ctx, Mob npc, Blackboard bb) {
        Location target = bb.get(targetKey).orElse(null);
        if (target == null || target.getWorld() == null) {
            ctx.navigation().stop(npc);
            log.info("Target location is null or has no world");
            return Status.FAILURE;
        }

        if (!target.getWorld().equals(npc.getWorld())) {
            ctx.navigation().stop(npc);
            return Status.FAILURE;
        }

        if (ctx.navigation().isAt(npc, target, stopDistance)) {
            ctx.navigation().stop(npc);
            log.info("Reached target " + ToString.of(target));
            return Status.SUCCESS;
        }

        if (ctx.navigation().isNavigating(npc)) {
            log.info("Already navigating to " + ToString.of(target));
            return Status.RUNNING;
        }

        boolean started = ctx.navigation().moveTo(npc, target, speed, bb);
        log.info("Moving to " + ToString.of(target));
        return started ? Status.RUNNING : Status.FAILURE;
    }

    @Override
    protected void onAbort(BtContext ctx, Mob npc, Blackboard bb) {
        ctx.navigation().stop(npc);
    }

    @Override
    protected void onEnd(BtContext ctx, Mob npc, Blackboard bb, Status result) {
        ctx.navigation().stop(npc);
    }
}