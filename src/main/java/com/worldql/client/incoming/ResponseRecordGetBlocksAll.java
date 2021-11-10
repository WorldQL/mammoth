package com.worldql.client.incoming;

import WorldQLFB.StandardEvents.Update;
import com.worldql.client.DirectionalUtilities;
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
    private ResponseRecordGetBlocksAll() {

    }

    public static void process(Update update, Plugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String worldName = update.worldName();
                if (worldName == null) return;

                // What you want to schedule goes here
                World world = Bukkit.getWorld(worldName);
                if (world == null) return;

                for (int i = 0; i < update.paramsLength(); i++) {
                    String blockCustomData = update.params(i);
                    if (blockCustomData == null) return;

                    //WorldQLClient.logger.info(block_data);
                    double blockX = update.numericalParams(i * 3);
                    double blockY = update.numericalParams(i * 3 + 1);
                    double blockZ = update.numericalParams(i * 3 + 2);

                    String[] blockDataArray = blockCustomData.split("\n");
                    BlockData blockData = Bukkit.getServer().createBlockData(blockDataArray[0]);
                    Location l = new Location(world, blockX, blockY, blockZ);
                    world.getBlockAt(l).setBlockData(blockData);

                    // we only get bed feet so we need to create the rest of the bed.
                    if (Tag.BEDS.isTagged(blockData.getMaterial())) {
                        Bed bed = (Bed) blockData;
                        l = l.add(bed.getFacing().getDirection());
                        DirectionalUtilities.setBed(world.getBlockAt(l), bed.getFacing(), blockData.getMaterial());
                    } else {
                        world.getBlockAt(l).setBlockData(blockData);
                    }


                    if (blockDataArray.length > 1) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                //WorldQLClient.logger.info("SIGN");
                                if (world.getBlockAt(
                                        new Location(world, blockX, blockY, blockZ)).getState() instanceof Sign sign) {
                                    for (int j = 1; j < blockDataArray.length; j++) {
                                        sign.setLine(j - 1, blockDataArray[j]);
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
