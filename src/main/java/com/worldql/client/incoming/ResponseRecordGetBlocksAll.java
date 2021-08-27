package com.worldql.client.incoming;

import WorldQLFB.StandardEvents.Update;
import com.worldql.client.DirectionalUtilities;
import com.worldql.client.WorldQLClient;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ResponseRecordGetBlocksAll {
    public static void process(Update update, Plugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                // What you want to schedule goes here
                World world = Bukkit.getWorld(update.worldName());
                for (int i = 0; i < update.paramsLength(); i++) {
                    String block_data = update.params(i);
                    //WorldQLClient.logger.info(block_data);
                    double blockx = update.numericalParams(i * 3);
                    double blocky = update.numericalParams(i * 3 + 1);
                    double blockz = update.numericalParams(i * 3 + 2);
                    String[] block_datas = block_data.split("\n");
                    BlockData blockData = Bukkit.getServer().createBlockData(block_datas[0]);
                    Location l = new Location(world, blockx, blocky, blockz);
                    world.getBlockAt(l).setBlockData(blockData);

                    // we only get bed feet so we need to create the rest of the bed.
                    if (Tag.BEDS.isTagged(blockData.getMaterial())) {
                        Bed bed = (Bed) blockData;
                        l = l.add(bed.getFacing().getDirection());
                        DirectionalUtilities.setBed(world.getBlockAt(l), bed.getFacing(), blockData.getMaterial());
                    } else {
                        world.getBlockAt(l).setBlockData(blockData);
                    }


                    if (block_datas.length > 1) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                //WorldQLClient.logger.info("SIGN");
                                if (world.getBlockAt(
                                        new Location(world, blockx, blocky, blockz)).getState() instanceof Sign) {
                                    Sign sign = (Sign) world.getBlockAt(
                                            new Location(world, blockx, blocky, blockz)).getState();
                                    for (int j = 1; j < block_datas.length; j++) {
                                        sign.setLine(j - 1, block_datas[j]);
                                    }
                                    sign.update();
                                }
                            }
                        }.runTaskLater(plugin, 2);
                    }
                }
            }

        }.runTask(plugin);
    }
}
