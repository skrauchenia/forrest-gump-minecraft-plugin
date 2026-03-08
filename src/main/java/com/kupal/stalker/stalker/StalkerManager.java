package com.kupal.stalker.stalker;

import com.kupal.stalker.StalkerPlugin;
import com.kupal.stalker.ai.Blackboard;
import com.kupal.stalker.ai.services.NavigationService;
import com.kupal.stalker.ai.services.SimpleSteeringNavigationService;
import com.kupal.stalker.services.MovementService;
import com.kupal.stalker.services.PrankService;
import com.kupal.stalker.services.VisionService;
import com.kupal.stalker.services.WorldInteractionService;
import com.kupal.stalker.stalker.tree.StalkerTrees;
import com.kupal.stalker.ai.minecraft.NpcBrain;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public final class StalkerManager {

    private final StalkerPlugin plugin;

    private BukkitTask task;
    private UUID npcId;
    private NpcBrain brain;

    public StalkerManager(StalkerPlugin plugin) {
        this.plugin = plugin;
    }

    public void spawnAtPlayer(Player player) {
        despawn();

        World w = player.getWorld();
        Location spawn = player.getLocation().clone().add(0, 0, -10);
        spawn.setY(w.getHighestBlockYAt(spawn) + 1);

        Villager v = w.spawn(spawn, Villager.class, ent -> {
            ent.setCustomName("...");
            ent.setCustomNameVisible(false);
            ent.setInvulnerable(false);
            ent.setAI(true);
            ent.setCollidable(true);
            ent.setRemoveWhenFarAway(false);
        });

        this.npcId = v.getUniqueId();

        VisionService vision = new VisionService(plugin.getConfig());
        MovementService movement = new MovementService(plugin.getConfig());
        WorldInteractionService worldInteraction = new WorldInteractionService(plugin.getConfig());
        PrankService pranks = new PrankService(plugin.getConfig(), worldInteraction);
        NavigationService navigation = new SimpleSteeringNavigationService();

        this.brain = new NpcBrain(
                plugin,
                v,
                StalkerTrees.build(plugin, vision, movement, pranks),
                new Blackboard(),
                navigation
        );

        start();
    }

    public void despawn() {
        stop();

        if (npcId != null) {
            Mob m = getNpc();
            if (m != null && !m.isDead()) {
                m.remove();
            }
        }

        npcId = null;
        brain = null;
    }

    public void start() {
        stop();
        if (npcId == null || brain == null) return;

        int period = Math.max(1, plugin.getConfig().getInt("tickPeriod", 5));
        plugin.getLogger().info("Stalker task period=" + period + " ticks");

        task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            long tick = 0;

            @Override
            public void run() {
                Mob npc = getNpc();
                if (npc == null || npc.isDead()) {
                    if (brain != null) {
                        brain.abortAll(tick);
                    }
                    stop();
                    return;
                }

                brain.tick(tick++);
            }
        }, 1L, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        if (brain != null) {
            brain.abortAll(0L);
        }
    }

    public void shutdown() {
        despawn();
    }

    private Mob getNpc() {
        if (npcId == null) return null;
        var e = Bukkit.getEntity(npcId);
        return e instanceof Mob mob ? mob : null;
    }

    public String status() {
        return "npc=" + npcId
                + ", running=" + (task != null)
                + ", brain=" + (brain != null ? "ok" : "null")
                + ", behavior=" + (brain != null ? brain.currentBehavior() : "n/a");
    }
}