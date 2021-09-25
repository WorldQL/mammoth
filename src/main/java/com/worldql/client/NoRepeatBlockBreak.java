package com.worldql.client;

import WorldQLFB.StandardEvents.Update;
import com.google.flatbuffers.FlatBufferBuilder;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Material;
import zmq.ZMQ;
import WorldQLFB.StandardEvents.Update;

import java.io.IOException;
import java.util.Objects;

import static com.worldql.client.listeners.PlayerBlockPlaceListener.createRoundedVec3;

public class NoRepeatBlockBreak {
    public static void sendInitBlockBreakMessage(BlockBreakEvent event) {
        BlockState blockState = event.getBlock().getState();
        String containerContentsBase64 = "";
        if (blockState instanceof Container) {
            ItemStack[] items = ((Container) blockState).getInventory().getContents();
            containerContentsBase64 = MinecraftUtil.itemStackArrayToBase64(items);
        }
        Location l = event.getBlock().getLocation();
        ItemStack[] itemDropsArray = event.getBlock().getDrops().toArray(new ItemStack[0]);
        String itemDropsBase64 = MinecraftUtil.itemStackArrayToBase64(itemDropsArray);

        FlatBufferBuilder builder = new FlatBufferBuilder(1024);

        int instruction = builder.createString("NoRepeat.BlockBreak");
        int worldName = builder.createString(event.getBlock().getWorld().getName());
        int blockData = builder.createString(event.getBlock().getBlockData().getAsString());
        int containerContents = builder.createString(containerContentsBase64);
        int itemDrops = builder.createString(itemDropsBase64);

        int[] paramsArray = {blockData, itemDrops, containerContents};
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
                    if (!update.params(2).equals("")) {
                        ItemStack[] containerDrops = MinecraftUtil.itemStackArrayFromBase64(update.params(2));
                        for (ItemStack item : containerDrops) {
                            if (item.getType() == Material.AIR){
                                ///If air, it needs to skip that item to avoid errors.
                                return;
                            }
                            world.dropItemNaturally(l, item);
                        }
                    }
                    for (ItemStack item : drop) {
                        if (item.getType() == Material.AIR){
                            ///If air, it needs to skip that item to avoid errors.
                            return;
                        }
                        world.dropItemNaturally(l, item);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.runTask(WorldQLClient.getPluginInstance());
    }
}
