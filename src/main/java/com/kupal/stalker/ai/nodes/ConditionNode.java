package com.kupal.stalker.ai.nodes;

import com.kupal.stalker.ai.Blackboard;
import com.kupal.stalker.ai.BtContext;
import com.kupal.stalker.ai.BtNode;
import com.kupal.stalker.ai.Status;
import org.bukkit.entity.Mob;

import java.util.function.BiPredicate;

public final class ConditionNode implements BtNode {
    private final BiPredicate<Mob, Blackboard> predicate;

    public ConditionNode(BiPredicate<Mob, Blackboard> predicate) {
        this.predicate = predicate;
    }

    @Override
    public Status tick(BtContext ctx, Mob mob, Blackboard bb) {
        return predicate.test(mob, bb) ? Status.SUCCESS : Status.FAILURE;
    }
}