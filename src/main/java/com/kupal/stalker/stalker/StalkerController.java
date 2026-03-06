package com.kupal.stalker.stalker;

import com.kupal.stalker.services.MovementService;
import com.kupal.stalker.services.PrankService;
import com.kupal.stalker.services.VisionService;
import org.bukkit.Location;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public final class StalkerController {

    private final VisionService vision;
    private final MovementService movement;
    private final PrankService pranks;

    private final double targetSearchRadius;
    private final double minDistance;
    private final double maxDistance;

    private StalkerState state = StalkerState.IDLE;

    public StalkerController(Configuration cfg,
                             VisionService vision,
                             MovementService movement,
                             PrankService pranks) {
        this.vision = vision;
        this.movement = movement;
        this.pranks = pranks;

        this.targetSearchRadius = cfg.getDouble("targetSearchRadius", 48.0);
        this.minDistance = cfg.getDouble("minDistance", 6.0);
        this.maxDistance = cfg.getDouble("maxDistance", 18.0);
    }

    /**
     * Target selection logic. For now, it's simple: the closest player within a radius. We can later improve it
     * by considering the player's line of sight, activity (moving/standing), or even "interest level"
     * (who looks towards the NPC more often).
     *
     * @param npc
     * @return
     */
    public Player pickTarget(Mob npc) {
        Player best = null;
        double bestDist2 = targetSearchRadius * targetSearchRadius;

        for (Player p : npc.getWorld().getPlayers()) {
            if (p.isDead() || !p.isOnline()) continue;
            double d2 = p.getLocation().distanceSquared(npc.getLocation());
            if (d2 < bestDist2) {
                bestDist2 = d2;
                best = p;
            }
        }
        return best;
    }

    /**
     * Docs in English:
     * Main NPC behaviour logic. Depending on the player's state (seeing/not seeing) and distance, we choose an action:
     * - If the player sees the NPC, it freezes (FREEZE).
     * - If the player does not see the NPC, it may start pranking (PRANKING).
     * - If the player is far, the NPC stalks (STALKING).
     * - If the player is too close, the NPC retreats (RETREAT).
     * - If the player is at a comfortable distance, the NPC observes (OBSERVING).
     *
     * @param npc
     * @param target
     */
    public void tick(Mob npc, Player target) {
        if (target == null) {
            state = StalkerState.IDLE;
            movement.stop(npc);
            return;
        }

        boolean playerSeesNpc = vision.playerSeesNpc(target, npc);
        double dist = target.getLocation().distance(npc.getLocation());

        if (playerSeesNpc) {
//
//            state = StalkerState.FREEZE;
//            movement.freeze(npc, target);
            // look up nearest tree and hide

            return;
        }

        // Player doesn't see
        if (pranks.maybeStartPrank(npc, target)) {
            state = StalkerState.PRANKING;
            movement.stop(npc);
            pranks.tick(npc, target);
            return;
        }

        if (dist > maxDistance) {
            state = StalkerState.STALKING;
            Location approach = movement.computeApproachPointBehindTarget(npc, target, 3.0);
            movement.moveTo(npc, approach, true);
        } else if (dist < minDistance) {
            state = StalkerState.RETREAT;
            Location retreat = movement.computeRetreatPoint(npc, target, 6.0);
            movement.moveTo(npc, retreat, false);
        } else {
            state = StalkerState.OBSERVING;
            Location loiter = movement.computeLoiterPoint(npc, target, 10.0);
            movement.moveTo(npc, loiter, false);
        }
    }

    public StalkerState getState() {
        return state;
    }
}