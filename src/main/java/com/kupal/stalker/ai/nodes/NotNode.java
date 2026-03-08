package com.kupal.stalker.ai.nodes;

import com.kupal.stalker.ai.*;
import org.bukkit.entity.Mob;

public final class NotNode implements BtNode {

    private final BtNode child;

    public NotNode(BtNode child) {
        this.child = child;
    }

    @Override
    public Status tick(BtContext ctx, Mob mob, Blackboard bb) {
        Status s = child.tick(ctx, mob, bb);
        return switch (s) {
            case SUCCESS -> Status.FAILURE;
            case FAILURE -> Status.SUCCESS;
            case RUNNING -> Status.RUNNING;
        };
    }
}