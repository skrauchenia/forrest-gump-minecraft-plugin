package com.kupal.stalker.stalker;

import com.kupal.stalker.StalkerPlugin;
import com.kupal.stalker.services.MovementService;
import com.kupal.stalker.services.PrankService;
import com.kupal.stalker.services.VisionService;
import com.kupal.stalker.stalker.state.StalkerState;
import org.bukkit.Location;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

@Deprecated
public final class StalkerController {

    private final VisionService vision;
    private final MovementService movement;
    private final PrankService pranks;

    private final double targetSearchRadius;
    private final double minDistance;
    private final double maxDistance;

    private final Logger logger;

    private StalkerState state = StalkerState.IDLE;

    public StalkerController(StalkerPlugin plugin,
                             VisionService vision,
                             MovementService movement,
                             PrankService pranks) {
        Configuration cfg = plugin.getConfig();

        this.vision = vision;
        this.movement = movement;
        this.pranks = pranks;

        this.targetSearchRadius = cfg.getDouble("targetSearchRadius", 48.0);
        this.minDistance = cfg.getDouble("minDistance", 6.0);
        this.maxDistance = cfg.getDouble("maxDistance", 18.0);
        this.logger = plugin.getLogger();
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
            logger.info("No target found, idling.");
            state = StalkerState.IDLE;
            movement.stop(npc);
            return;
        }

        // todo:
        // 1. player sees works incorrectly. When the stalker is behind a tree it still thinks a player sees it.
        // 2. choosing the nearest tree finds incorrect tree

        boolean playerSeesNpc = vision.playerSeesNpc(target, npc);
        double dist = target.getLocation().distance(npc.getLocation());

        if (playerSeesNpc) {
            // Try to hide behind a tree
            logger.info("Player sees NPC, Trying to hide.");
            Location hidePos = movement.findAndComputeTreeHidingPosition(npc, target, targetSearchRadius);
            if (hidePos != null) {
                state = StalkerState.HIDING;
                movement.moveTo(npc, hidePos, false, target);
            } else {
                logger.info("No tree found for hiding, freezing.");
                // Fallback: freeze if no tree found
                state = StalkerState.FREEZE;
                movement.freeze(npc, target);
            }
        } else {
            // Player doesn't see
            logger.info("Player sees NPC, Trying to hide.");
            if (pranks.maybeStartPrank(npc, target)) {
                state = StalkerState.PRANKING;
                movement.stop(npc);
                pranks.tick(npc, target);
                return;
            }

            if (dist > maxDistance) {
                state = StalkerState.STALKING;
                Location approach = movement.computeApproachPointBehindTarget(npc, target, 3.0);
                logger.info("Player is far, stalking. Moving to " + approach);
                movement.moveTo(npc, approach, true, target);
            } else if (dist < minDistance) {
                state = StalkerState.RETREAT;
                Location retreat = movement.computeRetreatPoint(npc, target, 6.0);
                logger.info("Player is far, stalking. Moving to " + retreat);
                movement.moveTo(npc, retreat, false, target);
            } else {
                state = StalkerState.OBSERVING;
                Location loiter = movement.computeLoiterPoint(npc, target, 10.0);
                logger.info("Player is far, stalking. Moving to " + loiter);
                movement.moveTo(npc, loiter, false, target);
            }
        }
    }

    public StalkerState getState() {
        return state;
    }
}