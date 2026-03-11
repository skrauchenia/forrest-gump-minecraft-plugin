package com.kupal.stalker.ai.minecraft;

import com.kupal.stalker.ai.*;
import com.kupal.stalker.ai.nodes.StatefulActionNode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;

public final class WanderActionNode extends StatefulActionNode {
    private final double radius;
    private final double stopDistance;
    private final double speed;

    public WanderActionNode(double radius, double stopDistance, double speed) {
        this.radius = radius;
        this.stopDistance = stopDistance;
        this.speed = speed;
    }

    @Override
    protected void onStart(BtContext ctx, Mob mob, Blackboard bb) {
        Location target = chooseWanderTarget(mob.getLocation(), radius);
        if (target != null) {
            bb.put(BbKeys.MOVE_TARGET, target);
        }
    }

    @Override
    protected Status onTick(BtContext ctx, Mob mob, Blackboard bb) {
        Location target = bb.get(BbKeys.MOVE_TARGET).orElse(null);
        if (target == null) {
            ctx.navigation().stop(mob);
            return Status.FAILURE;
        }

        if (ctx.navigation().isAt(mob, target, stopDistance)) {
            ctx.navigation().stop(mob);
            return Status.SUCCESS;
        }

        ctx.navigation().moveTo(mob, target, speed, bb);
        return Status.RUNNING;
    }

    @Override
    protected void onAbort(BtContext ctx, Mob mob, Blackboard bb) {
        ctx.navigation().stop(mob);
        bb.remove(BbKeys.MOVE_TARGET);
    }

    @Override
    protected void onEnd(BtContext ctx, Mob mob, Blackboard bb, Status result) {
        ctx.navigation().stop(mob);
        bb.remove(BbKeys.MOVE_TARGET);
    }

    private static Location chooseWanderTarget(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) return null;

        for (int i = 0; i < 20; i++) {
            double dx = (Math.random() * 2 - 1) * radius;
            double dz = (Math.random() * 2 - 1) * radius;
            Location raw = center.clone().add(dx, 0, dz);

            Location grounded = findGround(world, raw);
            if (grounded != null) return grounded;
        }
        return null;
    }

    private static Location findGround(World world, Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        for (int y = loc.getBlockY() + 4; y >= loc.getBlockY() - 8; y--) {
            Block feet = world.getBlockAt(x, y, z);
            Block head = world.getBlockAt(x, y + 1, z);
            Block ground = world.getBlockAt(x, y - 1, z);

            if (feet.isPassable() && head.isPassable() && !ground.isPassable()) {
                return new Location(world, x + 0.5, y, z + 0.5);
            }
        }
        return null;
    }
}