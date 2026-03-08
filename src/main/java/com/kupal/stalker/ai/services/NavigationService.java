package com.kupal.stalker.ai.services;

import org.bukkit.Location;
import org.bukkit.entity.Mob;

public interface NavigationService {
    void moveTo(Mob mob, Location target, double speed);

    void stop(Mob mob);

    boolean isAt(Mob mob, Location target, double distance);

    boolean isNavigating(Mob mob);
}