package com.kupal.stalker.ai.minecraft;

import com.kupal.stalker.ai.*;
import com.kupal.stalker.stalker.tree.StalkerBbKeys;
import com.kupal.stalker.util.IntComparison;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public class TargetDistanceCheckNode  implements BtNode {

    private final IntComparison predicate;
    private final int distance;
    private final BlackboardKey<Player> playerKey;
    private final Logger log =  Logger.getLogger(TargetDistanceCheckNode.class.getSimpleName());

    public TargetDistanceCheckNode(BlackboardKey<Player> playerKey, IntComparison predicate, int distance) {
        this.predicate = predicate;
        this.distance = distance;
        this.playerKey = playerKey;
    }

    @Override
    public Status tick(BtContext ctx, Mob npc, Blackboard bb) {
        Player target = bb.get(playerKey).orElse(null);
        if (target == null) {
            log.info("No %s saved.".formatted(playerKey.name()));
            return Status.FAILURE;
        }
        int distance = (int) target.getLocation().distance(npc.getLocation());
        boolean result = predicate.test(distance, this.distance);

        if  (result) {
            log.info("Found target %s is %s then %s blocks.".formatted(target.getName(), predicate.symbol(), this.distance));
            bb.put(StalkerBbKeys.DEBUG_REASON, "target " + predicate.symbol() + " than " + this.distance + " (" + distance + ")");
            return Status.SUCCESS;
        } else {
            log.info("Target %s is not %s then %s blocks.".formatted(target.getName(), predicate.symbol(), this.distance));
            return Status.FAILURE;
        }
    }
}
