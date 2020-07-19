package org.nqnl.mammothgameserver.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class NetherMethods {
    public static boolean isSafeLocation(Location location) {
        Block feet = location.getBlock();
        Block ground = feet.getRelative(BlockFace.DOWN);
        Block head = feet.getRelative(BlockFace.UP);
        if (!ground.getType().isSolid()) {
            return false; // not solid
        }
        if (head.getType() != Material.AIR) {
            return false;
        }
        if (feet.getType() != Material.AIR) {
            return false;
        }

        return true;
    }
    public static Location findSafeLocation(Location n) {
        if (n.getWorld().getName().equals("world")) {
            return n.getWorld().getHighestBlockAt(n).getLocation();
        }
        double z = n.getZ();

        for (double y = 5; y < 100; y++) {
            Location l = new Location(n.getWorld(), n.getX(), y, z);
            if (isSafeLocation(l)) {
                return l;
            }
            if (y == 99) {
                y = 5;
                z += 10;
            }
        }
        return null;
    }
}
