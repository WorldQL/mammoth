package com.worldql.client.listeners.utils;

import com.worldql.client.WorldQLClient;
import com.worldql.client.serialization.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

public class BlockTools {
    public static void scheduleSetBlockToAir(String worldName, Vec3D v) {
        Location l = new Location(Bukkit.getWorld(worldName), v.x(), v.y(), v.z());
        new BukkitRunnable() {
            @Override
            public void run() {
                l.getBlock().setType(Material.AIR);
            }

        }.runTask(WorldQLClient.pluginInstance);
    }
}
