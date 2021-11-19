package com.worldql.client.listeners.utils;

import com.worldql.client.WorldQLClient;
import com.worldql.client.serialization.Record;
import com.worldql.client.serialization.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class BlockTools {
    public static void setRecords(List<Record> records) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Record record : records) {
                    Vec3D p = record.position();
                    Block b = Bukkit.getWorld(record.worldName())
                            .getBlockAt((int)p.x(), (int)p.y(), (int)p.z());
                    BlockData bd = Bukkit.createBlockData(record.data());
                    b.setBlockData(bd);
                }
            }

        }.runTask(WorldQLClient.pluginInstance);
    }
}
