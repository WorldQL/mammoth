package com.worldql.client.listeners.utils;

import com.worldql.client.WorldQLClient;
import com.worldql.client.listeners.PlayerBreakBlockListener;
import com.worldql.client.serialization.Record;
import com.worldql.client.serialization.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.List;

public class BlockTools {
    public static void setRecords(List<Record> records, boolean isSelf) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Record record : records) {
                    Vec3D p = record.position();
                    Block b = Bukkit.getWorld(record.worldName())
                            .getBlockAt((int)p.x(), (int)p.y(), (int)p.z());
                    BlockData bd = Bukkit.createBlockData(record.data());
                    b.setBlockData(bd);

                    if (record.flex() != null && isSelf && PlayerBreakBlockListener.pendingDrops.contains(record.uuid())) {
                        PlayerBreakBlockListener.pendingDrops.remove(record.uuid());

                        ItemStack[] drops = new ItemStack[0];
                        try {
                            drops = PlayerBreakBlockListener.deserializeItemStack(record.flex());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        Location blockCenter = b.getLocation().add(0.5, 0.5, 0.5);
                        for (ItemStack item : drops) {
                            b.getWorld().dropItem(blockCenter, item);
                        }
                    }
                }
            }

        }.runTask(WorldQLClient.pluginInstance);
    }
}
