package com.kupal.stalker.services;

import org.bukkit.configuration.Configuration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class VisionService {

    private final double freezeDotThreshold;
    private final boolean requireLineOfSight;

    public VisionService(Configuration cfg) {
        this.freezeDotThreshold = cfg.getDouble("freezeDotThreshold", 0.65);
        this.requireLineOfSight = cfg.getBoolean("requireLineOfSight", true);
    }

    public boolean playerSeesNpc(Player player, LivingEntity npc) {
        if (requireLineOfSight && !player.hasLineOfSight(npc)) return false;

        Vector look = player.getEyeLocation().getDirection().normalize();
        Vector toNpc = npc.getLocation().toVector().subtract(player.getEyeLocation().toVector()).normalize();

        double dot = look.dot(toNpc);
        return dot >= freezeDotThreshold;
    }
}