package com.kupal.stalker.ai.nodes;

import com.kupal.stalker.ai.*;
import org.bukkit.entity.Mob;

public final class DebugLabelNode implements BtNode {
    private final String label;
    private final BtNode child;
    private final BlackboardKey<String> debugKey;

    public DebugLabelNode(String label, BlackboardKey<String> debugKey, BtNode child) {
        this.label = label;
        this.debugKey = debugKey;
        this.child = child;
    }

    @Override
    public Status tick(BtContext ctx, Mob mob, Blackboard bb) {
        Status status = child.tick(ctx, mob, bb);
        if (status == Status.SUCCESS || status == Status.RUNNING) {
            bb.put(debugKey, label);
        }
        return status;
    }
}