package com.kupal.stalker.stalker.tree;

import com.kupal.stalker.ai.*;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public final class RecentlyDamagedAndCloseNode implements BtNode {

    private final long panicDurationTicks;

    Logger log = Logger.getLogger("RecentlyDamagedAndCloseNode");

    public RecentlyDamagedAndCloseNode(long panicDurationTicks) {
        this.panicDurationTicks = panicDurationTicks;
    }

    @Override
    public Status tick(BtContext ctx, Mob npc, Blackboard bb) {
        log.info("Ticking recently damaged and close node for " + npc.getName());
        Long lastDamageTick = bb.get(StalkerBbKeys.LAST_DAMAGE_TICK).orElse(null);
        Player threat = bb.get(StalkerBbKeys.THREAT_PLAYER).orElse(null);

        if (lastDamageTick == null || threat == null || !threat.isOnline() || threat.isDead()) {
            log.info("Threat is absent");
            return Status.FAILURE;
        }

        long age = ctx.currentTick() - lastDamageTick;
        boolean recentlyDamaged = age <= panicDurationTicks;
        log.info("Recently damaged and close node for " + npc.getName());
        int distance = (int) threat.getLocation().distance(npc.getLocation());
        log.info("Distance to close node for " + npc.getName() + " is " + distance);
        boolean tooClose = threat.isOnline() && !threat.isDead() && distance < 40;
        boolean panic = recentlyDamaged && tooClose;
        log.info("RecentlyDamagedAndCloseNode: age=" + age + ", recentlyDamaged=" + recentlyDamaged + ", distance=" + distance + ", tooClose=" + tooClose);
        if (panic) {
            bb.put(StalkerBbKeys.DEBUG_REASON, "recently damaged by " + threat.getName() + " and distance is " + distance + ".");
            bb.put(StalkerBbKeys.LAST_DAMAGE_TICK, ctx.currentTick());
            return Status.SUCCESS;
        } else {
            return Status.FAILURE;
        }
    }
}