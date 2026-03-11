package com.kupal.stalker.util;

import org.bukkit.Location;

public class ToString {

    public static String of(Location location) {
        return "(%d,%d,%d)".formatted(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }
}
