package com.kupal.stalker.services;

import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public final class PrankService {

    private final boolean enablePranks;
    private final double chancePerTick;
    private final WorldInteractionService world;

    public PrankService(Configuration cfg, WorldInteractionService world) {
        this.enablePranks = cfg.getBoolean("enablePranks", false);
        this.chancePerTick = cfg.getDouble("prankChancePerTick", 0.02);
        this.world = world;
    }

    public boolean maybeStartPrank(Mob npc, Player target) {
        if (!enablePranks) return false;
        return ThreadLocalRandom.current().nextDouble() < chancePerTick;
    }

    public void tick(Mob npc, Player target) {
        // Here we can implement various pranks. For example:
        // world.openNearbyDoor(...), world.placeWater(...), world.buildStructure(...)
    }
}