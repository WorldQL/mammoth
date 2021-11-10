package com.worldql.client.listeners;

import WorldQLFB.StandardEvents.Update;
import WorldQLFB.StandardEvents.Vec3;
import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.NoRepeatBlockBreak;
import com.worldql.client.WorldQLClient;
import com.worldql.client.listeners.utils.BlockPlaceUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitRunnable;
import zmq.ZMQ;

import java.util.Optional;

public class PlayerBlockListener implements Listener {
    @EventHandler
    public void onPlayerBreakBlockEvent(BlockBreakEvent e) {
        Location l = e.getBlock().getLocation();
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);

        int instruction = builder.createString("MinecraftBlockBreak");
        int worldName = builder.createString(e.getBlock().getWorld().getName());
        int blockData = builder.createString(e.getBlock().getBlockData().getAsString());

        int[] paramsArray = {blockData};
        int params = Update.createParamsVector(builder, paramsArray);

        Update.startUpdate(builder);
        Update.addInstruction(builder, instruction);
        Update.addWorldName(builder, worldName);
        Update.addPosition(builder, createRoundedVec3(builder, l.getX(), l.getY(), l.getZ()));
        Update.addParams(builder, params);
        Update.addSenderid(builder, WorldQLClient.getPluginInstance().getZmqPortClientId());

        int blockUpdate = Update.endUpdate(builder);
        builder.finish(blockUpdate);

        byte[] buf = builder.sizedByteArray();
        WorldQLClient.getPluginInstance().getPushSocket().send(buf, ZMQ.ZMQ_DONTWAIT);

        NoRepeatBlockBreak.sendInitBlockBreakMessage(e);
    }

    @EventHandler
    public void onPlayerPlaceBlockEvent(BlockPlaceEvent e) {
        final Block[] relatives = {
                e.getBlock().getRelative(1, 0, 0),
                e.getBlock().getRelative(-1, 0, 0),
                e.getBlock().getRelative(0, 1, 0),
                e.getBlock().getRelative(0, -1, 0),
                e.getBlock().getRelative(0, 0, 1),
                e.getBlock().getRelative(0, 0, -1)
        };

        final Location[] relativeLocations = {
                e.getBlock().getRelative(1, 0, 0).getLocation(),
                e.getBlock().getRelative(-1, 0, 0).getLocation(),
                e.getBlock().getRelative(0, 1, 0).getLocation(),
                e.getBlock().getRelative(0, -1, 0).getLocation(),
                e.getBlock().getRelative(0, 0, 1).getLocation(),
                e.getBlock().getRelative(0, 0, -1).getLocation(),
        };

        // Handle server-joined blocks (fences, beds, glass panes, etc...)
        // for whatever reason this doesn't work for beds.
        new BukkitRunnable() {
            @Override
            public void run() {
                Block[] newRelatives = {
                        e.getBlock().getRelative(1, 0, 0),
                        e.getBlock().getRelative(-1, 0, 0),
                        e.getBlock().getRelative(0, 1, 0),
                        e.getBlock().getRelative(0, -1, 0),
                        e.getBlock().getRelative(0, 0, 1),
                        e.getBlock().getRelative(0, 0, -1)
                };

                for (int i = 0; i < 6; i++) {
                    // seems like the 1.17.1 update made this entire "wait 3 ticks" trick worthless. door adjacent blocks
                    // update before the event handler is called.
                    if (!relatives[i].equals(newRelatives[i]) ||
                            (newRelatives[i].getBlockData().getAsString().contains("door") && newRelatives[i].getBlockData().getAsString().contains("half=upper"))
                    ) {
                        BlockPlaceUtils.sendPacket(newRelatives[i], Optional.empty());
                    }
                }
            }
        }.runTaskLater(WorldQLClient.getPluginInstance(), 3);

        BlockPlaceUtils.sendPacket(e.getBlock(), Optional.empty());
    }

    public static int createRoundedVec3(FlatBufferBuilder builder, double x, double y, double z) {
        return Vec3.createVec3(builder, Math.round(x), Math.round(y), Math.round(z));
    }
}
