package com.kupal.stalker.ai.minecraft;

import com.kupal.stalker.ai.*;
import org.bukkit.World;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public final class CanSeePlayerNode implements BtNode {
    private final double maxDistance;

    public CanSeePlayerNode(double maxDistance) {
        this.maxDistance = maxDistance;
    }

    @Override
    public Status tick(BtContext ctx, Mob mob, Blackboard bb) {
        World world = mob.getWorld();

        Player nearestVisible = null;
        double bestDistSq = maxDistance * maxDistance;

        for (Player player : world.getPlayers()) {
            if (!player.isOnline() || player.isDead()) continue;
            if (player.getGameMode().name().equals("SPECTATOR")) continue;

            double distSq = player.getLocation().distanceSquared(mob.getLocation());
            if (distSq > bestDistSq) continue;
            if (!mob.hasLineOfSight(player)) continue;

            nearestVisible = player;
            bestDistSq = distSq;
        }

        if (nearestVisible == null) {
            bb.remove(BbKeys.THREAT_PLAYER);
            return Status.FAILURE;
        }

        bb.put(BbKeys.THREAT_PLAYER, nearestVisible);
        bb.put(BbKeys.LAST_SEEN_PLAYER_LOCATION, nearestVisible.getLocation().clone());
        return Status.SUCCESS;
    }
}