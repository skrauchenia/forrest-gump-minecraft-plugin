package com.kupal.stalker.ai.services;

import com.kupal.stalker.ai.Blackboard;
import org.bukkit.Location;
import org.bukkit.entity.Mob;

public interface NavigationService {
    boolean moveTo(Mob mob, Location target, double speed, Blackboard blackboard);
    void stop(Mob mob);
    boolean isAt(Mob mob, Location target, double distance);
    boolean isNavigating(Mob mob);
}