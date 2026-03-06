package com.kupal.stalker.services;

import org.bukkit.Location;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class MovementService {

    private final double approachSpeed;
    private final double observeSpeed;

    public MovementService(Configuration cfg) {
        this.approachSpeed = cfg.getDouble("approachSpeed", 1.15);
        this.observeSpeed = cfg.getDouble("observeSpeed", 0.80);
    }

    public void stop(Mob npc) {
        try {
            npc.getPathfinder().stopPathfinding();
        } catch (Throwable ignored) {
        }
        npc.setVelocity(new Vector(0, 0, 0));
    }

    public void freeze(Mob npc, Player target) {
        stop(npc);
        // in English: is allowed to slightly "look" on a player, but doesn't move
        lookAt(npc, target.getEyeLocation());
    }

    public void moveTo(Mob npc, Location loc, boolean aggressive) {
        double speed = aggressive ? approachSpeed : observeSpeed;
        try {
            npc.getPathfinder().moveTo(loc, speed);
        } catch (Throwable t) {
            // fallback: teleport/nothing
        }
    }

    public Location computeApproachPointBehindTarget(Mob npc, Player target, double behind) {
        Vector back = target.getLocation().getDirection().normalize().multiply(-behind);
        Location base = target.getLocation().clone().add(back);
        return adjustToGround(base);
    }

    public Location computeRetreatPoint(Mob npc, Player target, double away) {
        Vector awayVec = npc.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(away);
        Location base = npc.getLocation().clone().add(awayVec);
        return adjustToGround(base);
    }

    public Location computeLoiterPoint(Mob npc, Player target, double radius) {
        Vector dir = target.getLocation().getDirection().normalize();
        Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize().multiply(radius);
        Location base = target.getLocation().clone().add(side);
        return adjustToGround(base);
    }

    private Location adjustToGround(Location loc) {
        var w = loc.getWorld();
        if (w == null) return loc;
        int y = w.getHighestBlockYAt(loc);
        loc.setY(y + 1);
        return loc;
    }

    private void lookAt(Mob npc, Location where) {
        Location l = npc.getLocation();
        Vector dir = where.toVector().subtract(l.toVector());
        l.setDirection(dir);
        npc.teleport(l); // simple way to set yaw/pitch
    }
}