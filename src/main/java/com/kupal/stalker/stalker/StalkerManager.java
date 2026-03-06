package com.kupal.stalker.stalker;

import com.kupal.stalker.StalkerPlugin;
import com.kupal.stalker.services.MovementService;
import com.kupal.stalker.services.PrankService;
import com.kupal.stalker.services.VisionService;
import com.kupal.stalker.services.WorldInteractionService;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;
import java.util.UUID;

public final class StalkerManager {

    private final StalkerPlugin plugin;

    private BukkitTask task;
    private UUID npcId;
    private StalkerController controller;

    public StalkerManager(StalkerPlugin plugin) {
        this.plugin = plugin;
    }

    public void spawnAtPlayer(Player player) {
        despawn();

        World w = player.getWorld();
        Location spawn = player.getLocation().clone().add(0, 0, -10); // стартовая грубая точка
        spawn.setY(w.getHighestBlockYAt(spawn) + 1);

        Villager v = w.spawn(spawn, Villager.class, ent -> {
            ent.setCustomName("...");
            ent.setCustomNameVisible(false);
//            ent.setSilent(true);
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

        this.controller = new StalkerController(plugin.getConfig(), vision, movement, pranks);

        // По умолчанию сразу запускаем
        start();
    }

    public void despawn() {
        stop();
        if (npcId != null) {
            Mob m = getNpc();
            if (m != null && !m.isDead()) m.remove();
        }
        npcId = null;
        controller = null;
    }

    public void start() {
        stop();
        if (npcId == null || controller == null) return;

        int period = Math.max(1, plugin.getConfig().getInt("tickPeriod", 5));

        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Mob npc = getNpc();
            if (npc == null || npc.isDead()) return;

            Player target = controller.pickTarget(npc);
            controller.tick(npc, target);
        }, 1L, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void shutdown() {
        despawn();
    }

    public String status() {
        return "npc=" + npcId
                + ", running=" + (task != null)
                + ", controller=" + (controller != null ? "ok" : "null");
    }

    private Mob getNpc() {
        if (npcId == null) return null;
        var e = Bukkit.getEntity(npcId);
        if (e instanceof Mob mob) return mob;
        return null;
    }
}