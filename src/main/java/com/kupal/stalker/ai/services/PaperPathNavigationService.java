package com.kupal.stalker.ai.services;

import com.destroystokyo.paper.entity.Pathfinder;
import com.kupal.stalker.ai.Blackboard;
import com.kupal.stalker.stalker.tree.StalkerBbKeys;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public final class PaperPathNavigationService implements NavigationService {

    @Override
    public boolean moveTo(Mob mob, Location target, double speed, Blackboard blackboard) {
        if (mob == null || target == null || target.getWorld() == null) {
            return false;
        }
        if (!mob.getWorld().equals(target.getWorld())) {
            return false;
        }

        Pathfinder pathfinder = mob.getPathfinder();

        // These flags are part of Paper's pathfinder API.
        // setCanFloat(false) helps avoid treating water as acceptable traversal.
        pathfinder.setCanFloat(false);
        pathfinder.setCanOpenDoors(false);
        pathfinder.setCanPassDoors(true);

        Pathfinder.PathResult path = pathfinder.findPath(target);
        if (path == null) {
            return false;
        }

        blackboard.get(StalkerBbKeys.TARGET_PLAYER).ifPresent(player ->
                VisualDebugService.drawPath(player, path.getPoints())
        );


        return pathfinder.moveTo(path, speed);
    }

    @Override
    public void stop(Mob mob) {
        if (mob == null) {
            return;
        }
        mob.getPathfinder().stopPathfinding();
    }

    @Override
    public boolean isAt(Mob mob, Location target, double distance) {
        if (mob == null || target == null || target.getWorld() == null) {
            return false;
        }
        if (!mob.getWorld().equals(target.getWorld())) {
            return false;
        }
        return mob.getLocation().distanceSquared(target) <= distance * distance;
    }

    @Override
    public boolean isNavigating(Mob mob) {
        return mob != null && mob.getPathfinder().hasPath();
    }
}