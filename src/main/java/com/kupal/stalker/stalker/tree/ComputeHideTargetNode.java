package com.kupal.stalker.stalker.tree;

import com.kupal.stalker.ai.*;
import com.kupal.stalker.services.MovementService;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public final class ComputeHideTargetNode implements BtNode {

    private final MovementService movement;
    private final double radius;

    public ComputeHideTargetNode(MovementService movement, double radius) {
        this.movement = movement;
        this.radius = radius;
    }

    @Override
    public Status tick(BtContext ctx, Mob npc, Blackboard bb) {
        Player target = bb.get(StalkerBbKeys.TARGET_PLAYER).orElse(null);
        if (target == null) return Status.FAILURE;

        Location old = bb.get(StalkerBbKeys.HIDE_TARGET).orElse(null);
        Location hidePos = movement.findAndComputeTreeHidingPosition(npc, target, radius);
        if (hidePos == null) {
            bb.remove(StalkerBbKeys.HIDE_TARGET);
            return Status.FAILURE;
        }

        if (old == null || old.distanceSquared(hidePos) > 0.01) {
            ctx.plugin().getLogger().info(
                    "[StalkerAI] new hide target: " + format(hidePos)
            );
        }

        bb.put(StalkerBbKeys.HIDE_TARGET, hidePos);
        bb.put(StalkerBbKeys.MOVE_TARGET, hidePos);
        return Status.SUCCESS;
    }

    private String format(Location loc) {
        return loc.getDirection() + ":" + loc.getX() + "," + loc.getY() + "," + loc.getZ();
    }
}