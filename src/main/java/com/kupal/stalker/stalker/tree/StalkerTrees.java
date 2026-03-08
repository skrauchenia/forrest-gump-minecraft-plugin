package com.kupal.stalker.stalker.tree;

import com.kupal.stalker.StalkerPlugin;
import com.kupal.stalker.ai.BtNode;
import com.kupal.stalker.ai.nodes.*;
import com.kupal.stalker.services.MovementService;
import com.kupal.stalker.services.PrankService;
import com.kupal.stalker.services.VisionService;

import java.util.List;

public final class StalkerTrees {

    private StalkerTrees() {
    }

    public static BtNode build(
            StalkerPlugin plugin,
            VisionService vision,
            MovementService movement,
            PrankService pranks
    ) {
        double targetSearchRadius = plugin.getConfig().getDouble("targetSearchRadius", 48.0);
        double minDistance = plugin.getConfig().getDouble("minDistance", 6.0);
        double maxDistance = plugin.getConfig().getDouble("maxDistance", 18.0);

        BtNode hideBranch = new DebugLabelNode(
                "HIDING",
                StalkerBbKeys.DEBUG_STATE,
                new SequenceNode(List.of(
                        new FindTargetPlayerNode(targetSearchRadius),
                        new TargetSeesNpcNode(vision),
                        new CooldownDecoratorNode(
                                new ComputeHideTargetNode(movement, targetSearchRadius),
                                plugin.getConfig().getInt("hideRecomputeCooldownTicks", 20)
                        ),
                        new MoveToTargetNode(StalkerBbKeys.HIDE_TARGET, 1.5, 0.25)
                )));

        BtNode prankBranch = new SequenceNode(List.of(
                new FindTargetPlayerNode(targetSearchRadius),
                new NotNode(new TargetSeesNpcNode(vision)),
                new PrankNode(pranks)
        ));

        BtNode stalkBranch = new DebugLabelNode(
                "STALKING",
                StalkerBbKeys.DEBUG_STATE,
                new SequenceNode(List.of(
                        new FindTargetPlayerNode(targetSearchRadius),
                        new NotNode(new TargetSeesNpcNode(vision)),
                        new TargetDistanceGreaterThanNode(maxDistance),
                        new CooldownDecoratorNode(
                                new ComputeApproachTargetNode(movement, 3.0),
                                plugin.getConfig().getInt("approachTargetCooldownTicks", 20)
                        ),
                        new MoveToTargetNode(StalkerBbKeys.APPROACH_TARGET, 1.5, 0.22)
                )));

        BtNode retreatBranch = new SequenceNode(List.of(
                new FindTargetPlayerNode(targetSearchRadius),
                new NotNode(new TargetSeesNpcNode(vision)),
                new TargetDistanceLessThanNode(minDistance),
                new CooldownDecoratorNode(
                        new ComputeRetreatTargetNode(movement, 6.0),
                        plugin.getConfig().getInt("retreatRecomputeCooldownTicks", 20)
                ),
                new MoveToTargetNode(StalkerBbKeys.RETREAT_TARGET, 1.5, 0.23)
        ));

        BtNode observeBranch = new DebugLabelNode(
                "OBSERVING",
                StalkerBbKeys.DEBUG_STATE,
                new SequenceNode(List.of(
                        new FindTargetPlayerNode(targetSearchRadius),
                        new NotNode(new TargetSeesNpcNode(vision)),
                        new CooldownDecoratorNode(
                                new ComputeLoiterTargetNode(movement, 10.0),
                                plugin.getConfig().getInt("loiterRecomputeCooldownTicks", 50)
                        ),
                        new MoveToTargetNode(StalkerBbKeys.LOITER_TARGET, 1.5, 0.18)
                )));

        return new SelectorNode(List.of(
                hideBranch,
//                prankBranch,
                stalkBranch,
                retreatBranch,
                observeBranch
        ));
    }
}