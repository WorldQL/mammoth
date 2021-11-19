package com.worldql.client;

import WorldQLFB_OLD.StandardEvents.Update;
import com.google.flatbuffers.FlatBufferBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Material;
import zmq.ZMQ;

import java.io.IOException;
import java.util.Objects;

import static com.worldql.client.listeners.PlayerBreakBlockListener.createRoundedVec3;

public class NoRepeatBlockBreak {
    public static void sendInitBlockBreakMessage(BlockBreakEvent event) {
        BlockState blockState = event.getBlock().getState();

        // check whether player is in creative mode and if the block isn't a container
        if (String.valueOf(event.getPlayer().getGameMode()).equals("CREATIVE") && !(event.getBlock() instanceof Container)) {
            // prevent item from dropping if the above conditions are met
            return;
        }

        Location l = event.getBlock().getLocation();
        ItemStack[] itemDropsArray = event.getBlock().getDrops().toArray(new ItemStack[0]);

        String itemDropsBase64 = MinecraftUtil.itemStackArrayToBase64(itemDropsArray);

        FlatBufferBuilder builder = new FlatBufferBuilder(1024);

        int instruction = builder.createString("NoRepeat.BlockBreak");
        int worldName = builder.createString(event.getBlock().getWorld().getName());
        int blockData = builder.createString(event.getBlock().getBlockData().getAsString());
        int itemDrops = builder.createString(itemDropsBase64);

        int[] paramsArray = {blockData, itemDrops};
        int params = Update.createParamsVector(builder, paramsArray);

        Update.startUpdate(builder);
        Update.addInstruction(builder, instruction);
        Update.addWorldName(builder, worldName);
        Update.addPosition(builder, createRoundedVec3(builder, l.getX(), l.getY(), l.getZ()));
        Update.addParams(builder, params);
        Update.addSenderid(builder, WorldQLClient.getPluginInstance().getZmqPortClientId());

        int blockupdate = Update.endUpdate(builder);
        builder.finish(blockupdate);

        byte[] buf = builder.sizedByteArray();
        WorldQLClient.getPluginInstance().getPushSocket().send(buf, ZMQ.ZMQ_DONTWAIT);
    }

    public static void spawnDrops(Update update) {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getServer().getWorld(Objects.requireNonNull(update.worldName()));
                Location l = new Location(world,
                        update.position().x(), update.position().y(), update.position().z());
                try {
                    ItemStack[] drop = MinecraftUtil.itemStackArrayFromBase64(update.params(1));
                    if (!update.params(1).equals("")) {
                        ItemStack[] containerDrops = MinecraftUtil.itemStackArrayFromBase64(update.params(1));
                        for (ItemStack item : containerDrops) {
                            if (item.getType() == Material.AIR){
                                ///If air, it needs to skip that item to avoid errors.
                                return;
                            }
                            world.dropItemNaturally(l, item);
                        }
                    } else {
                        for (ItemStack item : drop) {
                            if (item.getType() == Material.AIR){
                                ///If air, it needs to skip that item to avoid errors.
                                return;
                            }
                            world.dropItemNaturally(l, item);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.runTask(WorldQLClient.getPluginInstance());
    }
}
