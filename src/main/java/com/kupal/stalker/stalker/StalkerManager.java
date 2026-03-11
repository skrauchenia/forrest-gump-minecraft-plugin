package com.kupal.stalker.stalker;

import com.kupal.stalker.StalkerPlugin;
import com.kupal.stalker.ai.Blackboard;
import com.kupal.stalker.ai.services.NavigationService;
import com.kupal.stalker.ai.services.PaperPathNavigationService;
import com.kupal.stalker.ai.services.VisualDebugService;
import com.kupal.stalker.services.MovementService;
import com.kupal.stalker.services.PrankService;
import com.kupal.stalker.services.VisionService;
import com.kupal.stalker.services.WorldInteractionService;
import com.kupal.stalker.stalker.tree.StalkerBbKeys;
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

import org.bukkit.entity.Entity;

public final class StalkerManager {

    private final StalkerPlugin plugin;

    private BukkitTask task;
    private UUID npcId;
    private NpcBrain brain;
    private long currentServerTick = 0L;

    public StalkerManager(StalkerPlugin plugin) {
        this.plugin = plugin;
    }

    public void spawnAtPlayer(Player player) {
        despawn();

        this.currentServerTick = 0L;

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
        MovementService movement = new MovementService(plugin);
        WorldInteractionService worldInteraction = new WorldInteractionService(plugin.getConfig());
        PrankService pranks = new PrankService(plugin.getConfig(), worldInteraction);
        NavigationService navigation = new PaperPathNavigationService();

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

        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Mob npc = getNpc();
            if (npc == null || npc.isDead()) {
                if (brain != null) {
                    brain.abortAll(currentServerTick);
                }
                stop();
                return;
            }

            brain.tick(currentServerTick);
            currentServerTick += period;
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

    public void handleDamage(Entity entity, Player attacker) {
        Mob npc = getNpc();
        if (npc == null || brain == null) return;
        if (!npc.getUniqueId().equals(entity.getUniqueId())) return;

        brain.blackboard().put(StalkerBbKeys.THREAT_PLAYER, attacker);
        brain.blackboard().put(StalkerBbKeys.LAST_DAMAGE_TICK, currentServerTick);
    }

    public void handleDeath(Entity entity, Player killer) {
        Mob npc = getNpc();
        if (npc == null) return;
        if (!npc.getUniqueId().equals(entity.getUniqueId())) return;

        plugin.getLogger().info("Stalker died. Killer=" + (killer != null ? killer.getName() : "null"));

        Player respawnNear = killer;
        if (respawnNear == null && brain != null) {
            respawnNear = brain.blackboard().get(StalkerBbKeys.THREAT_PLAYER).orElse(null);
        }
        if (respawnNear == null && brain != null) {
            respawnNear = brain.blackboard().get(StalkerBbKeys.TARGET_PLAYER).orElse(null);
        }
        if (respawnNear == null) {
            respawnNear = findNearestPlayerToEntity(entity);
        }

        stop();
        npcId = null;
        brain = null;

        Player finalRespawnNear = respawnNear;
        if (finalRespawnNear != null && finalRespawnNear.isOnline() && !finalRespawnNear.isDead()) {
            Bukkit.getScheduler().runTask(plugin, () -> spawnNearPlayer(finalRespawnNear, 20.0, 30));
        }
    }

    private Player findNearestPlayerToEntity(Entity entity) {
        Player best = null;
        double bestDist2 = Double.MAX_VALUE;

        for (Player p : entity.getWorld().getPlayers()) {
            if (!p.isOnline() || p.isDead()) continue;

            double d2 = p.getLocation().distanceSquared(entity.getLocation());
            if (d2 < bestDist2) {
                bestDist2 = d2;
                best = p;
            }
        }
        return best;
    }

    public void spawnNearPlayer(Player player, double radius, int attempts) {
        despawn();

        Location spawn = findSpawnNearPlayer(player, radius, attempts);
        if (spawn == null) {
            plugin.getLogger().warning("Failed to find spawn location near player " + player.getName());
            return;
        }

        Villager v = spawn.getWorld().spawn(spawn, Villager.class, ent -> {
            ent.setCustomName("...");
            ent.setCustomNameVisible(false);
            ent.setInvulnerable(false);
            ent.setAI(true);
            ent.setCollidable(true);
            ent.setRemoveWhenFarAway(false);
        });

        this.npcId = v.getUniqueId();

        VisionService vision = new VisionService(plugin.getConfig());
        MovementService movement = new MovementService(plugin);
        WorldInteractionService worldInteraction = new WorldInteractionService(plugin.getConfig());
        PrankService pranks = new PrankService(plugin.getConfig(), worldInteraction);
        NavigationService navigation = new PaperPathNavigationService();

        this.brain = new NpcBrain(
                plugin,
                v,
                StalkerTrees.build(plugin, vision, movement, pranks),
                new Blackboard(),
                navigation
        );

        this.currentServerTick = 0L;
        start();
    }

    private Location findSpawnNearPlayer(Player player, double radius, int attempts) {
        World world = player.getWorld();

        for (int i = 0; i < attempts; i++) {
            double angle = Math.random() * Math.PI * 2.0;
            double r = radius * (0.6 + Math.random() * 0.4);

            double dx = Math.cos(angle) * r;
            double dz = Math.sin(angle) * r;

            Location candidate = player.getLocation().clone().add(dx, 0, dz);
            int y = world.getHighestBlockYAt(candidate);
            Location spawn = new Location(world, candidate.getX(), y + 1, candidate.getZ());

            double heightDiff = Math.abs(spawn.getY() - player.getLocation().getY());
            if (heightDiff > 3.0) {
                continue; // too high/low, skip
            }

            if (spawn.getBlock().isPassable() && spawn.clone().add(0, 1, 0).getBlock().isPassable()) {
                return spawn;
            }
        }

        Location fallback = player.getLocation().clone().add(0, 0, -10);
        fallback.setY(world.getHighestBlockYAt(fallback) + 1);
        return fallback;
    }

    public String status() {
        Location currentLocation = brain.currentPosition();
        int x = currentLocation.getBlockX();
        int y = currentLocation.getBlockY();
        int z = currentLocation.getBlockZ();
        return "npc=" + npcId
                + ", running=" + (task != null)
                + ", brain=" + (brain != null ? "ok" : "null")
                + ", behavior=" + (brain != null ? brain.currentBehavior() : "n/a")
                + ", location=(" + x + "," + y + "," + z + ")";
    }
}