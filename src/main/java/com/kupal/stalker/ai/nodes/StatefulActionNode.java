package com.kupal.stalker.ai.nodes;

import com.kupal.stalker.ai.Blackboard;
import com.kupal.stalker.ai.BtContext;
import com.kupal.stalker.ai.BtNode;
import com.kupal.stalker.ai.Status;
import org.bukkit.entity.Mob;

public abstract class StatefulActionNode implements BtNode {
    private boolean active = false;

    @Override
    public final Status tick(BtContext ctx, Mob mob, Blackboard bb) {
        if (!active) {
            onStart(ctx, mob, bb);
            active = true;
        }

        ctx.runtime().markActive(this);

        Status result = onTick(ctx, mob, bb);

        if (result != Status.RUNNING) {
            try {
                onEnd(ctx, mob, bb, result);
            } finally {
                active = false;
            }
        }

        return result;
    }

    protected void onStart(BtContext ctx, Mob mob, Blackboard bb) {
    }

    protected abstract Status onTick(BtContext ctx, Mob mob, Blackboard bb);

    protected void onEnd(BtContext ctx, Mob mob, Blackboard bb, Status result) {
    }

    protected void onAbort(BtContext ctx, Mob mob, Blackboard bb) {
    }

    public final void abort(BtContext ctx, Mob mob, Blackboard bb) {
        if (!active) return;

        try {
            onAbort(ctx, mob, bb);
        } finally {
            active = false;
        }
    }

    public final boolean isActive() {
        return active;
    }
}