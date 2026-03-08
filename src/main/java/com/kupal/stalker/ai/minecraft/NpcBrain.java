package com.kupal.stalker.ai.minecraft;

import com.kupal.stalker.ai.*;
import com.kupal.stalker.ai.nodes.StatefulActionNode;
import com.kupal.stalker.ai.runtime.BtRuntime;
import com.kupal.stalker.ai.services.NavigationService;
import com.kupal.stalker.stalker.tree.StalkerBbKeys;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class NpcBrain {

    private final Plugin plugin;
    private final Mob mob;
    private final BtNode root;
    private final Blackboard blackboard;
    private final NavigationService navigation;
    private String lastDebugState = "NONE";
    private long lastStateChangeTick = -1;

    private Set<StatefulActionNode> activeLastTick =
            Collections.newSetFromMap(new IdentityHashMap<>());

    public NpcBrain(
            Plugin plugin,
            Mob mob,
            BtNode root,
            Blackboard blackboard,
            NavigationService navigation
    ) {
        this.plugin = plugin;
        this.mob = mob;
        this.root = root;
        this.blackboard = blackboard;
        this.navigation = navigation;
    }

    public void tick(long currentTick) {
        if (!mob.isValid() || mob.isDead()) {
            abortAll(currentTick);
            return;
        }

        BtRuntime runtime = new BtRuntime();
        BtContext ctx = new BtContext(plugin, currentTick, navigation, runtime);

        root.tick(ctx, mob, blackboard);
        String currentState = blackboard.get(StalkerBbKeys.DEBUG_STATE).orElse("IDLE");
        String reason = blackboard.get(StalkerBbKeys.DEBUG_REASON).orElse("n/a");

        if (!currentState.equals(lastDebugState)) {
            plugin.getLogger().info(
                    "[StalkerAI] state switch: " + lastDebugState + " -> " + currentState +
                            " at tick=" + currentTick +
                            " reason=" + reason
            );
            lastDebugState = currentState;
            lastStateChangeTick = currentTick;
        }

        Set<StatefulActionNode> activeThisTick = runtime.activeThisTick();

        for (StatefulActionNode oldNode : activeLastTick) {
            if (!activeThisTick.contains(oldNode)) {
                oldNode.abort(ctx, mob, blackboard);
            }
        }

        activeLastTick = Collections.newSetFromMap(new IdentityHashMap<>());
        activeLastTick.addAll(activeThisTick);
    }

    public void abortAll(long currentTick) {
        BtRuntime runtime = new BtRuntime();
        BtContext ctx = new BtContext(plugin, currentTick, navigation, runtime);

        for (StatefulActionNode node : activeLastTick) {
            node.abort(ctx, mob, blackboard);
        }

        activeLastTick = Collections.newSetFromMap(new IdentityHashMap<>());
        navigation.stop(mob);
    }

    public Blackboard blackboard() {
        return blackboard;
    }

    public String currentBehavior() {
        return blackboard.get(StalkerBbKeys.DEBUG_STATE).orElse("IDLE");
    }
}