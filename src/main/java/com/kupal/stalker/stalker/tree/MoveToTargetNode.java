package com.kupal.stalker.stalker.tree;

import com.kupal.stalker.ai.*;
import com.kupal.stalker.ai.nodes.StatefulActionNode;
import org.bukkit.Location;
import org.bukkit.entity.Mob;

public final class MoveToTargetNode extends StatefulActionNode {

    private final BlackboardKey<Location> targetKey;
    private final double stopDistance;
    private final double speed;

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
            return Status.FAILURE;
        }

        if (!target.getWorld().equals(npc.getWorld())) {
            ctx.navigation().stop(npc);
            return Status.FAILURE;
        }

        if (ctx.navigation().isAt(npc, target, stopDistance)) {
            ctx.navigation().stop(npc);
            return Status.SUCCESS;
        }

        ctx.navigation().moveTo(npc, target, speed);
        return Status.RUNNING;
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