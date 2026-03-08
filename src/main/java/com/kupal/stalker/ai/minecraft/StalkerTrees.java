package com.kupal.stalker.ai.minecraft;

import com.kupal.stalker.ai.*;
import com.kupal.stalker.ai.nodes.*;

import java.util.List;

public final class StalkerTrees {
    private StalkerTrees() {}

    public static BtNode fleeOrWanderTree() {
        BtNode flee = new SequenceNode(List.of(
                new CanSeePlayerNode(16.0),
                new CooldownDecoratorNode(
                        new FindCoverNode(10, 4, 30),
                        10L
                ),
                new MoveToBlackboardLocationNode(BbKeys.COVER_LOCATION, 1.5, 0.25)
        ));

        BtNode wander = new WanderActionNode(8.0, 1.2, 0.18);

        return new SelectorNode(List.of(
                flee,
                wander
        ));
    }
}