package com.kupal.stalker.ai;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class BbKeys {
    private BbKeys() {}

    public static final BlackboardKey<Player> THREAT_PLAYER =
            BlackboardKey.of("threat_player", Player.class);

    public static final BlackboardKey<Location> COVER_LOCATION =
            BlackboardKey.of("cover_location", Location.class);

    public static final BlackboardKey<Location> MOVE_TARGET =
            BlackboardKey.of("move_target", Location.class);

    public static final BlackboardKey<Location> LAST_SEEN_PLAYER_LOCATION =
            BlackboardKey.of("last_seen_player_location", Location.class);
}