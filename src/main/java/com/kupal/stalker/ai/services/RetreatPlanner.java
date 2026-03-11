package com.kupal.stalker.ai.services;

import com.destroystokyo.paper.entity.Pathfinder;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public final class RetreatPlanner {

    private final State state = new State();
    private final Logger log = Logger.getLogger(RetreatPlanner.class.getSimpleName());

    /**
     * Вызывай периодически.
     * Метод сам решает: продолжать идти к старой цели или искать новую.
     */
    public Optional<Location> tick(Mob npc, Player threat) {
        Objects.requireNonNull(npc, "npc");
        Objects.requireNonNull(threat, "threat");

        if (!npc.isValid() || !threat.isValid() || npc.isDead() || threat.isDead()) {
            clear();
            return Optional.empty();
        }

        Location npcLoc = npc.getLocation();

        updateStuckState(npcLoc);

        boolean shouldReplan = shouldReplan(npc, threat);

        if (shouldReplan) {
            RetreatCandidate best = chooseRetreatCandidate(npc, threat);
            if (best != null) {
                state.currentTarget = best.location.clone();
                state.currentPath = best.path;
                state.lastScore = best.score;
                state.ticksSinceReplan = 0;
                state.failedReplans = 0;

                boolean started = npc.getPathfinder().moveTo(best.path, 1.0);
                if (!started) {
                    rememberFailedTarget(best.location);
                    state.currentTarget = null;
                    state.currentPath = null;
                    return Optional.empty();
                }
            } else {
                state.currentTarget = null;
                state.currentPath = null;
                state.lastScore = Double.NEGATIVE_INFINITY;
                state.failedReplans++;
                return Optional.empty();
            }
        } else {
            state.ticksSinceReplan++;
        }

        if (state.currentTarget == null) {
            return Optional.empty();
        }

        if (!isStillReasonableTarget(npc, threat, state.currentTarget)) {
            rememberFailedTarget(state.currentTarget);
            state.currentTarget = null;
            return Optional.empty();
        }

        if (state.currentPath != null) {
            boolean started = npc.getPathfinder().moveTo(state.currentPath, 1.0);
            if (!started) {
                rememberFailedTarget(state.currentTarget);
                state.currentTarget = null;
                state.currentPath = null;
                return Optional.empty();
            }
        }

        return Optional.of(state.currentTarget.clone());
    }

    public void clear() {
        state.currentTarget = null;
        state.currentPath = null;
        state.lastNpcLocation = null;
        state.stuckTicks = 0;
        state.ticksSinceReplan = 0;
        state.failedReplans = 0;
        state.recentlyFailedTargets.clear();
        state.recentVisited.clear();
        state.lastScore = Double.NEGATIVE_INFINITY;
    }

    // =========================
    // Core planning
    // =========================

    private RetreatCandidate chooseRetreatCandidate(Mob npc, Player threat) {
        Location npcLoc = npc.getLocation();
        Location threatLoc = threat.getLocation();
        World world = npcLoc.getWorld();

        Vector away = horizontal(npcLoc.toVector().subtract(threatLoc.toVector()));
        if (away.lengthSquared() < 1.0e-8) {
            away = randomHorizontalUnitVector();
        } else {
            away.normalize();
        }

        List<RetreatCandidate> candidates = new ArrayList<>();

        double[] preferredAngles = {0, 18, -18, 35, -35, 55, -55, 80, -80, 110, -110, 145, -145, 180};
        double[] preferredRadii = {10.0, 8.0, 6.0, 4.0};

        for (double radius : preferredRadii) {
            for (double angleDeg : preferredAngles) {
                Vector dir = rotateYaw(away, angleDeg);
                Location raw = npcLoc.clone().add(dir.clone().multiply(radius));
                Location adjusted = adjustToGround(raw, npc);

                if (adjusted != null) {
                    RetreatCandidate candidate = evaluateCandidate(npc, threat, adjusted, away);
                    if (candidate != null) {
                        candidates.add(candidate);
                    }
                }
            }
        }

        // fallback: dense full-circle sampling on smaller radii
        if (candidates.isEmpty()) {
            log.info("No candidate found for stalker. Falling back");
            double[] fallbackRadii = {5.0, 3.5, 2.0};
            for (double radius : fallbackRadii) {
                for (int deg = 0; deg < 360; deg += 20) {
                    Vector dir = rotateYaw(new Vector(1, 0, 0), deg);
                    Location raw = npcLoc.clone().add(dir.multiply(radius));
                    Location adjusted = adjustToGround(raw, npc);

                    if (adjusted == null) {
                        continue;
                    }

                    RetreatCandidate candidate = evaluateCandidate(npc, threat, adjusted, away);
                    if (candidate != null) {
                        candidates.add(candidate);
                    }
                }
            }
        }

        return candidates.stream()
                .max(Comparator.comparingDouble(c -> c.score))
                .orElse(null);
    }

    private RetreatCandidate evaluateCandidate(Mob npc, Player threat, Location candidateLoc, Vector away) {
        Location npcLoc = npc.getLocation();
        Location threatLoc = threat.getLocation();

        if (!sameWorld(npcLoc, candidateLoc)) {
            return null;
        }

        if (!canStandAt(candidateLoc, npc)) {
            return null;
        }

        HazardAssessment targetHazard = assessHazard(candidateLoc, npc);
        if (targetHazard.hardReject) {
            return null;
        }

        if (isRecentlyFailed(candidateLoc)) {
            return null;
        }

        Pathfinder pathfinder = npc.getPathfinder();
        Pathfinder.PathResult path = pathfinder.findPath(candidateLoc);
        if (path == null) {
            return null;
        }

        PathAssessment pathAssessment = assessPaperPath(path, threat, npc);
        if (!pathAssessment.reachable) {
            return null;
        }

        Location finalPoint = resolveFinalPoint(path, candidateLoc);
        if (finalPoint == null) {
            finalPoint = candidateLoc;
        }

        double startDistToThreat = horizontalDistance(npcLoc, threatLoc);
        double endDistToThreat = horizontalDistance(finalPoint, threatLoc);
        double distGain = endDistToThreat - startDistToThreat;

        Vector moveDir = horizontal(finalPoint.toVector().subtract(npcLoc.toVector()));
        if (moveDir.lengthSquared() > 1.0e-8) {
            moveDir.normalize();
        }

        double alignment = moveDir.dot(away);
        double losBonus = hasLineOfSight(threatLoc, finalPoint) ? 0.0 : 1.0;

        double visitedPenalty = recentlyVisitedPenalty(finalPoint);
        double failedPenalty = recentlyFailedPenalty(finalPoint);

        double score =
                4.5 * distGain +
                        2.2 * alignment -
                        0.45 * pathAssessment.approxLength -
                        3.5 * pathAssessment.exposurePenalty -
                        8.0 * targetHazard.softPenalty -
                        2.5 * visitedPenalty -
                        8.0 * failedPenalty +
                        2.0 * losBonus;

        if (distGain < 0) {
            score += 2.0 * distGain;
        }

        if (state.stuckTicks >= 10) {
            score += 1.2 * (1.0 - Math.abs(alignment));
        }

        return new RetreatCandidate(finalPoint.clone(), path, score);
    }

    private Location resolveFinalPoint(Pathfinder.PathResult path, Location fallback) {
        try {
            Location finalPoint = path.getFinalPoint();
            if (finalPoint != null) {
                return finalPoint;
            }
        } catch (Throwable ignored) {
            // На случай несовпадения версии API
        }

        List<Location> points = path.getPoints();
        if (points != null && !points.isEmpty()) {
            return points.get(points.size() - 1);
        }

        return fallback;
    }

    private PathAssessment assessPaperPath(Pathfinder.PathResult path, Player threat, Mob npc) {
        List<Location> points = path.getPoints();
        if (points == null || points.isEmpty()) {
            return PathAssessment.unreachable();
        }

        double length = 0.0;
        double exposurePenalty = 0.0;

        Location prev = npc.getLocation();

        for (Location point : points) {
            if (point == null) {
                return PathAssessment.unreachable();
            }

            if (!sameWorld(prev, point)) {
                return PathAssessment.unreachable();
            }

            if (!canStandAt(point, npc)) {
                return PathAssessment.unreachable();
            }

            HazardAssessment hazard = assessHazard(point, npc);
            if (hazard.hardReject) {
                return PathAssessment.unreachable();
            }

            exposurePenalty += hazard.softPenalty;

            double dy = point.getY() - prev.getY();
            if (dy > 1.25) {
                return PathAssessment.unreachable();
            }

            if (dy < -2.25) {
                return PathAssessment.unreachable();
            }

            length += prev.distance(point);

            // Доп. штраф за точки, которые идут слишком близко к игроку
            double dThreat = horizontalDistance(point, threat.getLocation());
            if (dThreat < 4.0) {
                exposurePenalty += (4.0 - dThreat) * 1.5;
            }

            prev = point;
        }

        return new PathAssessment(true, length, exposurePenalty);
    }

    private boolean shouldReplan(Mob npc, Player threat) {
        if (state.currentTarget == null || state.currentPath == null) {
            return true;
        }

        if (state.stuckTicks >= 20) {
            rememberFailedTarget(state.currentTarget);
            return true;
        }

        if (state.ticksSinceReplan >= 40) {
            return true;
        }

        Pathfinder.PathResult current = npc.getPathfinder().getCurrentPath();
        if (current == null && npc.getLocation().distanceSquared(state.currentTarget) > 2.0 * 2.0) {
            return true;
        }

        if (npc.getLocation().distanceSquared(state.currentTarget) <= 2.0 * 2.0) {
            rememberVisited(state.currentTarget);
            return true;
        }

        Location threatLoc = threat.getLocation();
        double currentTargetThreatDist = horizontalDistance(state.currentTarget, threatLoc);
        double npcThreatDist = horizontalDistance(npc.getLocation(), threatLoc);

        return currentTargetThreatDist < npcThreatDist - 1.5;
    }

    private boolean isStillReasonableTarget(Mob npc, Player threat, Location target) {
        if (target == null) {
            return false;
        }

        if (!sameWorld(npc.getLocation(), target)) {
            return false;
        }

        if (!canStandAt(target, npc)) {
            return false;
        }

        HazardAssessment hazard = assessHazard(target, npc);
        return !hazard.hardReject;
    }

    // =========================
    // Movement / Path heuristics
    // =========================

    /**
     * Очень упрощённая проверка пути:
     * - шагами идём по прямому отрезку,
     * - на каждом шаге пытаемся "посадить" точку на землю,
     * - отвергаем путь, если видим воду/лаву/огонь/провал/непроходимую точку.
     * <p>
     * Это не заменяет настоящий pathfinder, но даёт дешёвый локальный фильтр.
     */
    private PathAssessment assessPathRoughly(Location from, Location to, Mob npc) {
        double distance = from.distance(to);
        int steps = Math.max(2, (int) Math.ceil(distance / 1.0));

        double exposurePenalty = 0.0;
        Location prev = from.clone();

        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            Location sample = lerp(from, to, t);
            Location grounded = adjustToGround(sample, npc);

            if (grounded == null) {
                return PathAssessment.unreachable();
            }

            if (!canStandAt(grounded, npc)) {
                return PathAssessment.unreachable();
            }

            HazardAssessment hazard = assessHazard(grounded, npc);
            if (hazard.hardReject) {
                return PathAssessment.unreachable();
            }

            exposurePenalty += hazard.softPenalty;

            double dy = grounded.getY() - prev.getY();
            if (dy > 1.25) {
                return PathAssessment.unreachable();
            }

            if (dy < -2.25) {
                return PathAssessment.unreachable();
            }

            prev = grounded;
        }

        return new PathAssessment(true, distance, exposurePenalty);
    }

    // =========================
    // Grounding / standability / hazards
    // =========================

    /**
     * Пытается найти ближайшую подходящую "поверхность" около raw.
     * Сначала ищет вниз, потом немного вверх.
     */
    private Location adjustToGround(Location raw, Mob npc) {
        World world = raw.getWorld();
        if (world == null) {
            return null;
        }

        int baseX = raw.getBlockX();
        int baseY = raw.getBlockY();
        int baseZ = raw.getBlockZ();

        // сначала вниз
        for (int dy = 0; dy <= 6; dy++) {
            Location candidate = centered(baseX, baseY - dy, baseZ, world);
            if (canStandAt(candidate, npc)) {
                return candidate;
            }
        }

        // потом чуть вверх
        for (int dy = 1; dy <= 2; dy++) {
            Location candidate = centered(baseX, baseY + dy, baseZ, world);
            if (canStandAt(candidate, npc)) {
                return candidate;
            }
        }

        return null;
    }

    private boolean canStandAt(Location loc, Mob npc) {
        World world = loc.getWorld();
        if (world == null) {
            return false;
        }

        Block feet = world.getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        Block head = world.getBlockAt(loc.getBlockX(), loc.getBlockY() + 1, loc.getBlockZ());
        Block ground = world.getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());

        if (!isWalkableGround(ground.getType())) {
            return false;
        }

        if (isBlockedForBody(feet.getType())) {
            return false;
        }

        if (isBlockedForBody(head.getType())) {
            return false;
        }

        if (feet.isLiquid() || head.isLiquid() || ground.isLiquid()) {
            return false;
        }

        // Минимальная проверка "не на краю страшного провала".
        if (hasDangerousDropNearby(loc, 2, 4)) {
            return false;
        }

        return true;
    }

    private HazardAssessment assessHazard(Location loc, Mob npc) {
        World world = loc.getWorld();
        if (world == null) {
            return HazardAssessment.reject();
        }

        Block feet = world.getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        Block head = world.getBlockAt(loc.getBlockX(), loc.getBlockY() + 1, loc.getBlockZ());
        Block ground = world.getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());

        double softPenalty = 0.0;

        if (isHardHazard(feet.getType()) || isHardHazard(head.getType()) || isHardHazard(ground.getType())) {
            return HazardAssessment.reject();
        }

        if (isSoftHazardNearby(loc, 1)) {
            softPenalty += 1.2;
        }

        if (hasDangerousDropNearby(loc, 1, 3)) {
            softPenalty += 2.0;
        }

        return HazardAssessment.ok(softPenalty);
    }

    private boolean hasDangerousDropNearby(Location loc, int radius, int maxSafeDrop) {
        World world = loc.getWorld();
        if (world == null) {
            return true;
        }

        int x0 = loc.getBlockX();
        int y0 = loc.getBlockY();
        int z0 = loc.getBlockZ();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                int x = x0 + dx;
                int z = z0 + dz;

                int drop = 0;
                for (int dy = 1; dy <= maxSafeDrop + 1; dy++) {
                    Block below = world.getBlockAt(x, y0 - dy, z);
                    if (below.getType().isSolid()) {
                        break;
                    }
                    drop++;
                }

                if (drop > maxSafeDrop) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isSoftHazardNearby(Location loc, int radius) {
        World world = loc.getWorld();
        if (world == null) {
            return true;
        }

        int x0 = loc.getBlockX();
        int y0 = loc.getBlockY();
        int z0 = loc.getBlockZ();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Material type = world.getBlockAt(x0 + dx, y0 + dy, z0 + dz).getType();
                    if (isHardHazard(type)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isWalkableGround(Material type) {
        if (!type.isSolid()) {
            return false;
        }

        return switch (type) {
            case LAVA, MAGMA_BLOCK, CACTUS, CAMPFIRE, SOUL_CAMPFIRE -> false;
            default -> true;
        };
    }

    private boolean isBlockedForBody(Material type) {
        if (type.isAir()) {
            return false;
        }

        if (type == Material.TALL_GRASS || type == Material.SHORT_GRASS || type == Material.FERN) {
            return false;
        }

        return type.isSolid() || type == Material.WATER || type == Material.LAVA;
    }

    private boolean isHardHazard(Material type) {
        return switch (type) {
            case WATER,
                 LAVA,
                 FIRE,
                 SOUL_FIRE,
                 MAGMA_BLOCK,
                 CAMPFIRE,
                 SOUL_CAMPFIRE,
                 CACTUS,
                 SWEET_BERRY_BUSH,
                 POWDER_SNOW -> true;
            default -> false;
        };
    }

    // =========================
    // LOS
    // =========================

    private boolean hasLineOfSight(Location from, Location to) {
        World world = from.getWorld();
        if (world == null || !sameWorld(from, to)) {
            return false;
        }

        Vector dir = to.toVector().subtract(from.toVector());
        double len = dir.length();
        if (len < 1.0e-8) {
            return true;
        }

        RayTraceResult hit = world.rayTraceBlocks(
                from.clone().add(0, 1.62, 0),
                dir.normalize(),
                len,
                FluidCollisionMode.ALWAYS,
                true
        );

        return hit == null;
    }

    private void updateStuckState(Location npcLoc) {
        if (state.lastNpcLocation == null || !sameWorld(state.lastNpcLocation, npcLoc)) {
            state.lastNpcLocation = npcLoc.clone();
            state.stuckTicks = 0;
            return;
        }

        double moved = npcLoc.distance(state.lastNpcLocation);
        if (moved < 0.20) {
            // TODO: adjust according to our ai tick
            state.stuckTicks++;
        } else {
            state.stuckTicks = 0;
            state.lastNpcLocation = npcLoc.clone();
        }
    }

    private void rememberFailedTarget(Location loc) {
        if (loc != null) {
            if (state.recentlyFailedTargets.size() >= 12) {
                state.recentlyFailedTargets.removeFirst();
            }
            state.recentlyFailedTargets.addLast(loc.clone());
        }
    }

    private void rememberVisited(Location loc) {
        if (loc != null) {
            if (state.recentVisited.size() >= 16) {
                state.recentVisited.removeFirst();
            }
            state.recentVisited.addLast(loc.clone());
        }
    }

    private boolean isRecentlyFailed(Location loc) {
        for (Location failed : state.recentlyFailedTargets) {
            if (sameWorld(failed, loc) && failed.distanceSquared(loc) <= 2.25) {
                return true;
            }
        }
        return false;
    }

    private double recentlyFailedPenalty(Location loc) {
        double penalty = 0.0;
        for (Location failed : state.recentlyFailedTargets) {
            if (sameWorld(failed, loc)) {
                double d2 = failed.distanceSquared(loc);
                if (d2 < 16.0) {
                    penalty += (16.0 - d2) / 16.0;
                }
            }
        }
        return penalty;
    }

    private double recentlyVisitedPenalty(Location loc) {
        double penalty = 0.0;
        for (Location visited : state.recentVisited) {
            if (sameWorld(visited, loc)) {
                double d2 = visited.distanceSquared(loc);
                if (d2 < 9.0) {
                    penalty += (9.0 - d2) / 9.0;
                }
            }
        }
        return penalty;
    }

    // =========================
    // Math helpers
    // =========================

    private static Vector horizontal(Vector v) {
        return new Vector(v.getX(), 0.0, v.getZ());
    }

    private static double horizontalDistance(Location a, Location b) {
        if (!sameWorld(a, b)) {
            return Double.POSITIVE_INFINITY;
        }
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static Vector rotateYaw(Vector dir, double angleDeg) {
        Vector v = horizontal(dir.clone());
        if (v.lengthSquared() < 1.0e-8) {
            return randomHorizontalUnitVector();
        }
        v.normalize();

        double rad = Math.toRadians(angleDeg);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double x = v.getX();
        double z = v.getZ();

        double newX = x * cos - z * sin;
        double newZ = x * sin + z * cos;

        return new Vector(newX, 0.0, newZ).normalize();
    }

    private static Vector randomHorizontalUnitVector() {
        double angle = ThreadLocalRandom.current().nextDouble(0.0, Math.PI * 2.0);
        return new Vector(Math.cos(angle), 0.0, Math.sin(angle));
    }

    private static Location lerp(Location a, Location b, double t) {
        World world = a.getWorld();
        return new Location(
                world,
                a.getX() + (b.getX() - a.getX()) * t,
                a.getY() + (b.getY() - a.getY()) * t,
                a.getZ() + (b.getZ() - a.getZ()) * t
        );
    }

    private static Location centered(int blockX, int blockY, int blockZ, World world) {
        return new Location(world, blockX + 0.5, blockY, blockZ + 0.5);
    }

    private static boolean sameWorld(Location a, Location b) {
        return a != null && b != null && a.getWorld() != null && a.getWorld().equals(b.getWorld());
    }

    private static final class State {
        private Location currentTarget;
        private Pathfinder.PathResult currentPath;
        private Location lastNpcLocation;
        private int stuckTicks;
        private int ticksSinceReplan;
        private int failedReplans;
        private double lastScore;

        private final Deque<Location> recentlyFailedTargets = new ArrayDeque<>();
        private final Deque<Location> recentVisited = new ArrayDeque<>();
    }

    private static final class RetreatCandidate {
        private final Location location;
        private final Pathfinder.PathResult path;
        private final double score;

        private RetreatCandidate(Location location, Pathfinder.PathResult path, double score) {
            this.location = location;
            this.path = path;
            this.score = score;
        }
    }

    private static final class HazardAssessment {
        private final boolean hardReject;
        private final double softPenalty;

        private HazardAssessment(boolean hardReject, double softPenalty) {
            this.hardReject = hardReject;
            this.softPenalty = softPenalty;
        }

        private static HazardAssessment reject() {
            return new HazardAssessment(true, Double.POSITIVE_INFINITY);
        }

        private static HazardAssessment ok(double softPenalty) {
            return new HazardAssessment(false, softPenalty);
        }
    }

    private static final class PathAssessment {
        private final boolean reachable;
        private final double approxLength;
        private final double exposurePenalty;

        private PathAssessment(boolean reachable, double approxLength, double exposurePenalty) {
            this.reachable = reachable;
            this.approxLength = approxLength;
            this.exposurePenalty = exposurePenalty;
        }

        private static PathAssessment unreachable() {
            return new PathAssessment(false, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        }
    }
}