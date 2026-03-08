package com.kupal.stalker.ai.services;

import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class SimpleSteeringNavigationService implements NavigationService {
    private final Set<Mob> navigating =
            Collections.newSetFromMap(new IdentityHashMap<>());

    @Override
    public void moveTo(Mob mob, Location target, double speed) {
        if (target == null || target.getWorld() == null) return;
        if (!target.getWorld().equals(mob.getWorld())) return;

        Location current = mob.getLocation();
        Vector direction = target.toVector().subtract(current.toVector());
        direction.setY(0);

        if (direction.lengthSquared() < 1.0e-8) {
            stop(mob);
            return;
        }

        direction.normalize().multiply(speed);
        Vector oldVel = mob.getVelocity();
        mob.setVelocity(new Vector(direction.getX(), oldVel.getY(), direction.getZ()));

        Location look = current.clone();
        look.setDirection(target.toVector().subtract(current.toVector()));
        mob.teleport(look);

        navigating.add(mob);
    }

    @Override
    public void stop(Mob mob) {
        Vector oldVel = mob.getVelocity();
        mob.setVelocity(new Vector(0, oldVel.getY(), 0));
        navigating.remove(mob);
    }

    @Override
    public boolean isAt(Mob mob, Location target, double distance) {
        if (target == null || target.getWorld() == null) return false;
        if (!target.getWorld().equals(mob.getWorld())) return false;
        return mob.getLocation().distanceSquared(target) <= distance * distance;
    }

    @Override
    public boolean isNavigating(Mob mob) {
        return navigating.contains(mob);
    }
}