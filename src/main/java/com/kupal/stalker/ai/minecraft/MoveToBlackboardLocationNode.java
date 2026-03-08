package com.kupal.stalker.ai.minecraft;

import com.kupal.stalker.ai.*;
import com.kupal.stalker.ai.nodes.StatefulActionNode;
import org.bukkit.Location;
import org.bukkit.entity.Mob;

public final class MoveToBlackboardLocationNode extends StatefulActionNode {
    private final BlackboardKey<Location> key;
    private final double stopDistance;
    private final double speed;

    public MoveToBlackboardLocationNode(
            BlackboardKey<Location> key,
            double stopDistance,
            double speed
    ) {
        this.key = key;
        this.stopDistance = stopDistance;
        this.speed = speed;
    }

    @Override
    protected Status onTick(BtContext ctx, Mob mob, Blackboard bb) {
        Location target = bb.get(key).orElse(null);
        if (target == null || target.getWorld() == null) {
            ctx.navigation().stop(mob);
            return Status.FAILURE;
        }

        if (!target.getWorld().equals(mob.getWorld())) {
            ctx.navigation().stop(mob);
            return Status.FAILURE;
        }

        if (ctx.navigation().isAt(mob, target, stopDistance)) {
            ctx.navigation().stop(mob);
            return Status.SUCCESS;
        }

        ctx.navigation().moveTo(mob, target, speed);
        return Status.RUNNING;
    }

    @Override
    protected void onAbort(BtContext ctx, Mob mob, Blackboard bb) {
        ctx.navigation().stop(mob);
    }

    @Override
    protected void onEnd(BtContext ctx, Mob mob, Blackboard bb, Status result) {
        ctx.navigation().stop(mob);
    }
}