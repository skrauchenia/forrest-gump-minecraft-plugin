package com.kupal.stalker.ai.nodes;

import com.kupal.stalker.ai.Blackboard;
import com.kupal.stalker.ai.BtContext;
import com.kupal.stalker.ai.BtNode;
import com.kupal.stalker.ai.Status;
import org.bukkit.entity.Mob;

import java.util.List;

public final class SelectorNode implements BtNode {
    private final List<BtNode> children;

    public SelectorNode(List<BtNode> children) {
        this.children = List.copyOf(children);
    }

    @Override
    public Status tick(BtContext ctx, Mob mob, Blackboard bb) {
        for (BtNode child : children) {
            Status status = child.tick(ctx, mob, bb);
            if (status == Status.SUCCESS || status == Status.RUNNING) {
                return status;
            }
        }
        return Status.FAILURE;
    }
}