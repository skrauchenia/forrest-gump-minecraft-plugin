package com.kupal.stalker;

import com.kupal.stalker.stalker.StalkerManager;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public final class StalkerCommand implements CommandExecutor, TabCompleter {

    private final StalkerPlugin plugin;
    private final StalkerManager manager;

    public StalkerCommand(StalkerPlugin plugin, StalkerManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
//        if (!p.hasPermission("stalker.admin")) {
//            p.sendMessage(ChatColor.RED + "No permission.");
//            return true;
//        }

        if (args.length == 0) {
            p.sendMessage(ChatColor.YELLOW + "Usage: /stalker <spawn|despawn|start|stop|status>");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "spawn" -> {
                manager.spawnAtPlayer(p);
                p.sendMessage(ChatColor.GREEN + "Spawned stalker.");
            }
            case "despawn" -> {
                manager.despawn();
                p.sendMessage(ChatColor.GREEN + "Despawned stalker.");
            }
            case "start" -> {
                manager.start();
                p.sendMessage(ChatColor.GREEN + "Stalker started.");
            }
            case "stop" -> {
                manager.stop();
                p.sendMessage(ChatColor.GREEN + "Stalker stopped.");
            }
            case "status" -> {
                p.sendMessage(ChatColor.AQUA + manager.status());
            }
            default -> p.sendMessage(ChatColor.YELLOW + "Unknown. Use: spawn|despawn|start|stop|status");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("spawn", "despawn", "start", "stop", "status");
        }
        return List.of();
    }
}