package com.kupal.stalker.stalker.tree;

import com.kupal.stalker.ai.*;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public final class FindTargetPlayerNode implements BtNode {

    private final double targetSearchRadius;

    public FindTargetPlayerNode(double targetSearchRadius) {
        this.targetSearchRadius = targetSearchRadius;
    }

    @Override
    public Status tick(BtContext ctx, Mob npc, Blackboard bb) {
        Player best = null;
        double bestDist2 = targetSearchRadius * targetSearchRadius;

        for (Player p : npc.getWorld().getPlayers()) {
            if (p.isDead() || !p.isOnline()) continue;

            double d2 = p.getLocation().distanceSquared(npc.getLocation());
            if (d2 < bestDist2) {
                bestDist2 = d2;
                best = p;
            }
        }

        if (best == null) {
            bb.remove(StalkerBbKeys.TARGET_PLAYER);
            return Status.FAILURE;
        }

        bb.put(StalkerBbKeys.TARGET_PLAYER, best);
        return Status.SUCCESS;
    }
}