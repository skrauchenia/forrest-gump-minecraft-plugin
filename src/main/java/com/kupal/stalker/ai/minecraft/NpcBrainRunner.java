package com.kupal.stalker.ai.minecraft;

import com.kupal.stalker.ai.Blackboard;
import com.kupal.stalker.ai.services.NavigationService;
import com.kupal.stalker.ai.services.SimpleSteeringNavigationService;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class NpcBrainRunner {
    private NpcBrainRunner() {
    }

    public static void start(Plugin plugin, Mob mob) {
        NavigationService navigation = new SimpleSteeringNavigationService();

        NpcBrain brain = new NpcBrain(
                plugin,
                mob,
                StalkerTrees.fleeOrWanderTree(),
                new Blackboard(),
                navigation
        );

        new BukkitRunnable() {
            long tick = 0;

            @Override
            public void run() {
                if (!mob.isValid() || mob.isDead()) {
                    brain.abortAll(tick);
                    cancel();
                    return;
                }

                brain.tick(tick++);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}