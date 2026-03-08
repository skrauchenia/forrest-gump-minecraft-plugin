package com.kupal.stalker.ai.nodes;

import com.kupal.stalker.ai.Blackboard;
import com.kupal.stalker.ai.BtContext;
import com.kupal.stalker.ai.BtNode;
import com.kupal.stalker.ai.Status;
import org.bukkit.entity.Mob;

public final class CooldownDecoratorNode implements BtNode {
    private final BtNode child;
    private final long cooldownTicks;

    private long nextAllowedTick = Long.MIN_VALUE;
    private Status cachedStatus = Status.FAILURE;

    public CooldownDecoratorNode(BtNode child, long cooldownTicks) {
        this.child = child;
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public Status tick(BtContext ctx, Mob mob, Blackboard bb) {
        if (ctx.currentTick() < nextAllowedTick) {
            return cachedStatus;
        }

        cachedStatus = child.tick(ctx, mob, bb);
        nextAllowedTick = ctx.currentTick() + cooldownTicks;
        return cachedStatus;
    }
}