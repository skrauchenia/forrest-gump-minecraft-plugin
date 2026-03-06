package com.kupal.stalker;

import com.kupal.stalker.stalker.StalkerManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class StalkerPlugin extends JavaPlugin {

    private StalkerManager stalkerManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.stalkerManager = new StalkerManager(this);

        PluginCommand cmd = getCommand("stalker");
        if (cmd != null) {
            cmd.setExecutor(new StalkerCommand(this, stalkerManager));
            cmd.setTabCompleter(new StalkerCommand(this, stalkerManager));
        }

        getLogger().info("StalkerNPC enabled");
    }

    @Override
    public void onDisable() {
        if (stalkerManager != null) {
            stalkerManager.shutdown();
        }
        getLogger().info("StalkerNPC disabled");
    }
}