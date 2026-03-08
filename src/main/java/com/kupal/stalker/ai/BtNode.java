package com.kupal.stalker.ai;

import org.bukkit.entity.Mob;

public interface BtNode {
    Status tick(BtContext ctx, Mob mob, Blackboard bb);
}