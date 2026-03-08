package com.kupal.stalker.ai;

import com.kupal.stalker.ai.runtime.BtRuntime;
import com.kupal.stalker.ai.services.NavigationService;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.util.logging.Logger;

public final class BtContext {
    private final Plugin plugin;
    private final long currentTick;
    private final NavigationService navigation;
    private final BtRuntime runtime;

    public BtContext(
            Plugin plugin,
            long currentTick,
            NavigationService navigation,
            BtRuntime runtime
    ) {
        this.plugin = plugin;
        this.currentTick = currentTick;
        this.navigation = navigation;
        this.runtime = runtime;
    }

    public Plugin plugin() {
        return plugin;
    }

    public @NonNull Logger logger() {
        return plugin.getLogger();
    }

    public long currentTick() {
        return currentTick;
    }

    public NavigationService navigation() {
        return navigation;
    }

    public BtRuntime runtime() {
        return runtime;
    }
}