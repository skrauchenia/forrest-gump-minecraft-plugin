package com.kupal.stalker.stalker.tree;

import com.kupal.stalker.ai.*;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public final class FindTargetPlayerNode implements BtNode {

    private final double targetSearchRadius;
    private final Logger log = Logger.getLogger(FindTargetPlayerNode.class.getSimpleName());

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
            log.info("Found no target player within radius %.1f".formatted(targetSearchRadius));
            return Status.FAILURE;
        } else {
            bb.put(StalkerBbKeys.TARGET_PLAYER, best);
            log.info("Found target player %s at distance %.1f".formatted(best.getName(), bestDist));
            return Status.SUCCESS;
        }
    }
}