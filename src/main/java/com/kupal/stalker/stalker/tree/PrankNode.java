package com.kupal.stalker.stalker.tree;

import com.kupal.stalker.ai.*;
import com.kupal.stalker.ai.nodes.StatefulActionNode;
import com.kupal.stalker.services.PrankService;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public final class PrankNode extends StatefulActionNode {

    private final PrankService pranks;

    public PrankNode(PrankService pranks) {
        this.pranks = pranks;
    }

    @Override
    protected Status onTick(BtContext ctx, Mob npc, Blackboard bb) {
        Player target = bb.get(StalkerBbKeys.TARGET_PLAYER).orElse(null);
        if (target == null || !target.isOnline() || target.isDead()) {
            return Status.FAILURE;
        }

        if (!pranks.maybeStartPrank(npc, target)) {
            return Status.FAILURE;
        }

        pranks.tick(npc, target);
        return Status.RUNNING;
    }

    @Override
    protected void onAbort(BtContext ctx, Mob npc, Blackboard bb) {
        // add prank cleanup here later if needed
    }
}