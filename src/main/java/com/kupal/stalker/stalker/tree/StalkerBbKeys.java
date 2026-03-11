package com.kupal.stalker.stalker.tree;

import com.kupal.stalker.ai.BlackboardKey;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class StalkerBbKeys {
    private StalkerBbKeys() {}

    public static final BlackboardKey<String> DEBUG_STATE =
            BlackboardKey.of("debug_state", String.class);

    public static final BlackboardKey<String> DEBUG_REASON =
            BlackboardKey.of("debug_reason", String.class);

    public static final BlackboardKey<Player> TARGET_PLAYER =
            BlackboardKey.of("target_player", Player.class);

    public static final BlackboardKey<Location> HIDE_TARGET =
            BlackboardKey.of("hide_target", Location.class);

    public static final BlackboardKey<Location> MOVE_TARGET =
            BlackboardKey.of("move_target", Location.class);

    public static final BlackboardKey<Location> APPROACH_TARGET =
            BlackboardKey.of("approach_target", Location.class);

    public static final BlackboardKey<Location> RETREAT_TARGET =
            BlackboardKey.of("retreat_target", Location.class);

    public static final BlackboardKey<Location> LOITER_TARGET =
            BlackboardKey.of("loiter_target", Location.class);

    public static final BlackboardKey<Player> THREAT_PLAYER =
            BlackboardKey.of("threat_player", Player.class);

    public static final BlackboardKey<Long> LAST_DAMAGE_TICK =
            BlackboardKey.of("last_damage_tick", Long.class);

    public static final BlackboardKey<Location> PANIC_TARGET =
            BlackboardKey.of("panic_target", Location.class);
}