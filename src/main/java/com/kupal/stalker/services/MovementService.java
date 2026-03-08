package com.kupal.stalker.services;

import com.destroystokyo.paper.entity.Pathfinder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;

public final class MovementService {

    private final double approachSpeed;
    private final double observeSpeed;

    public MovementService(Configuration cfg) {
        this.approachSpeed = cfg.getDouble("approachSpeed", 1.15);
        this.observeSpeed = cfg.getDouble("observeSpeed", 0.80);
    }

    public void stop(Mob npc) {
        try {
            npc.getPathfinder().stopPathfinding();
        } catch (Throwable ignored) {
        }
        npc.setVelocity(new Vector(0, 0, 0));
    }

    public void freeze(Mob npc, Player target) {
        stop(npc);
        // is allowed to slightly "look" on a player, but doesn't move
        lookAt(npc, target.getEyeLocation());
    }

    public static void drawPath(Player viewer, List<Location> points) {
        var world = viewer.getWorld();
        for (Location p : points) {
            world.spawnParticle(
                    org.bukkit.Particle.DUST,
                    p.clone().add(0, 0.2, 0),
                    2,              // count
                    0.02, 0.02, 0.02,// spread
                    0,
                    new org.bukkit.Particle.DustOptions(org.bukkit.Color.AQUA, 1.0f)
            );
        }
    }

    public void moveTo(Mob npc, Location loc, boolean aggressive, Player player) {
        double speed = aggressive ? approachSpeed : observeSpeed;
        try {
            Pathfinder.PathResult path = npc.getPathfinder().findPath(loc);
            if (path != null) {
                if (player != null) {
                    drawPath(player, path.getPoints());
                }
                npc.getPathfinder().moveTo(path, speed);
            }
        } catch (Throwable t) {
            // fallback: teleport/nothing
        }
    }

    public Location computeApproachPointBehindTarget(Mob npc, Player target, double behind) {
        Vector back = target.getLocation().getDirection().normalize().multiply(-behind);
        Location base = target.getLocation().clone().add(back);
        return adjustToGround(base);
    }

    public Location computeRetreatPoint(Mob npc, Player target, double away) {
        Vector awayVec = npc.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(away);
        Location base = npc.getLocation().clone().add(awayVec);
        return adjustToGround(base);
    }

    public Location computeLoiterPoint(Mob npc, Player target, double radius) {
        Vector dir = target.getLocation().getDirection().normalize();
        Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize().multiply(radius);
        Location base = target.getLocation().clone().add(side);
        return adjustToGround(base);
    }

    private Location adjustToGround(Location loc) {
        var w = loc.getWorld();
        if (w == null) return loc;
        int y = w.getHighestBlockYAt(loc);
        loc.setY(y + 1);
        return loc;
    }

    private void lookAt(Mob npc, Location where) {
        Location l = npc.getLocation();
        Vector dir = where.toVector().subtract(l.toVector());
        l.setDirection(dir);
        npc.teleport(l); // simple way to set yaw/pitch
    }

    /**
     * Finds the nearest tree within a given radius and computes a hiding position
     * behind it relative to the player's vision direction.
     * The tree selection considers the player's position to ensure the NPC doesn't
     * get too close to the player while hiding.
     *
     * @param npc          the NPC entity
     * @param player       the target player
     * @param searchRadius the radius within which to search for trees
     * @return a Location to hide behind the tree, or null if no tree is found
     */
    public Location findAndComputeTreeHidingPosition(Mob npc, Player player, double searchRadius) {
        Location npcLoc = npc.getLocation();
        Location playerLoc = player.getLocation();
        double currentDistance = npcLoc.distance(playerLoc);


        Block bestTree = findBestTreeForHiding(npcLoc, playerLoc, searchRadius, currentDistance);
        if (bestTree == null) {
            return null;
        }

        // Compute hide position behind the tree, opposite to player's vision direction
        Vector playerLook = playerLoc.getDirection().normalize();
        Vector hideOffset = playerLook.multiply(2.5); // 2.5 blocks behind the tree from player's perspective

        Location hidePos = bestTree.getLocation().clone().add(0.5, 0, 0.5).add(hideOffset);
        return adjustToGround(hidePos);
    }


    /**
     * Finds the best tree for hiding that maintains a safe distance from the player.
     * The tree is selected based on:
     * 1. It should not bring the NPC significantly closer to the player
     * 2. It should be close to the NPC for quick access
     * 3. The hiding position should maintain at least 70% of the current distance to the player
     *
     * @param npcLoc          the NPC's current location
     * @param playerLoc       the player's location
     * @param searchRadius    the radius within which to search for trees
     * @param currentDistance the current distance between NPC and player
     * @return the best tree block for hiding, or null if no suitable tree is found
     */
    private Block findBestTreeForHiding(Location npcLoc, Location playerLoc, double searchRadius, double currentDistance) {
        Block bestTree = null;
        double bestScore = Double.MAX_VALUE;

        // Minimum acceptable distance from player (70% of current distance, but at least 4 blocks)
        double minAcceptableDistance = Math.max(4.0, currentDistance * 0.7);

        int radiusInt = (int) Math.ceil(searchRadius);
        int centerX = npcLoc.getBlockX();
        int centerY = npcLoc.getBlockY();
        int centerZ = npcLoc.getBlockZ();

        for (int x = centerX - radiusInt; x <= centerX + radiusInt; x++) {
            for (int y = centerY - radiusInt; y <= centerY + radiusInt; y++) {
                for (int z = centerZ - radiusInt; z <= centerZ + radiusInt; z++) {
                    Block block = npcLoc.getWorld().getBlockAt(x, y, z);
                    if (isTreeLog(block.getType())) {
                        // Compute the hiding position behind this tree
                        Vector playerLook = playerLoc.getDirection().normalize();
                        Vector hideOffset = playerLook.multiply(2.5);
                        Location potentialHidePos = block.getLocation().clone().add(0.5, 0, 0.5).add(hideOffset);

                        // Calculate distance from hiding position to player
                        double distanceToPlayer = potentialHidePos.distance(playerLoc);

                        // Skip trees that would bring us too close to the player
                        if (distanceToPlayer < minAcceptableDistance) {
                            continue;
                        }

                        // Calculate distance from NPC to tree (for scoring)
                        double distanceToTree = Math.sqrt(
                                Math.pow(x - centerX, 2) +
                                        Math.pow(y - centerY, 2) +
                                        Math.pow(z - centerZ, 2)
                        );

                        // Skip if outside search radius
                        if (distanceToTree > searchRadius) {
                            continue;
                        }

                        // Score: prefer trees that are closer to NPC but maintain good distance from player
                        // Lower score is better
                        double score = distanceToTree * 2.0 - (distanceToPlayer - minAcceptableDistance) * 0.5;

                        if (score < bestScore) {
                            bestScore = score;
                            bestTree = block;
                        }
                    }
                }
            }
        }

        return bestTree;
    }


    /**
     * Checks if a material is a log block (any type of tree log).
     *
     * @param material the material to check
     * @return true if the material is a log block
     */
    private boolean isTreeLog(Material material) {
        return material == Material.OAK_LOG ||
                material == Material.BIRCH_LOG ||
                material == Material.SPRUCE_LOG ||
                material == Material.JUNGLE_LOG ||
                material == Material.ACACIA_LOG ||
                material == Material.DARK_OAK_LOG ||
                material == Material.MANGROVE_LOG ||
                material == Material.CHERRY_LOG ||
                material == Material.PALE_OAK_LOG;
    }
}