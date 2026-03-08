package com.kupal.stalker.stalker.tree;

import com.kupal.stalker.ai.*;
import com.kupal.stalker.services.VisionService;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public final class TargetSeesNpcNode implements BtNode {

    private final VisionService vision;

    public TargetSeesNpcNode(VisionService vision) {
        this.vision = vision;
    }

    @Override
    public Status tick(BtContext ctx, Mob npc, Blackboard bb) {
        Player target = bb.get(StalkerBbKeys.TARGET_PLAYER).orElse(null);
        if (target == null || !target.isOnline() || target.isDead()) {
            return Status.FAILURE;
        }

        boolean sees = vision.playerSeesNpc(target, npc);
        if  (sees) {
            bb.put(StalkerBbKeys.DEBUG_REASON, "target sees npc");
        }

        return sees ? Status.SUCCESS : Status.FAILURE;
    }
}