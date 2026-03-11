package com.kupal.stalker.ai.services;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class VisualDebugService {

    public static void drawPath(Player viewer, List<Location> points) {
        var world = viewer.getWorld();
        for (Location p : points) {
            world.spawnParticle(
                    org.bukkit.Particle.DUST,
                    p.clone().add(0, 0.2, 0),
                    2,              // count
                    0.02, 0.02, 0.02,// spread
                    0,
                    new org.bukkit.Particle.DustOptions(org.bukkit.Color.AQUA, 1.0f)
            );
        }
    }
}
