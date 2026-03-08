package com.kupal.stalker.ai.runtime;

import com.kupal.stalker.ai.nodes.StatefulActionNode;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class BtRuntime {
    private final Set<StatefulActionNode> activeThisTick = Collections.newSetFromMap(new IdentityHashMap<>());

    public void markActive(StatefulActionNode node) {
        activeThisTick.add(node);
    }

    public Set<StatefulActionNode> activeThisTick() {
        return activeThisTick;
    }
}