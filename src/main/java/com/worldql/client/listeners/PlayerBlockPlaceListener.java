package com.worldql.client.listeners;

import WorldQLFB.StandardEvents.Update;
import WorldQLFB.StandardEvents.Vec3;
import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.NoRepeatBlockBreak;
import com.worldql.client.WorldQLClient;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitRunnable;
import zmq.ZMQ;

public class PlayerBlockPlaceListener implements Listener {
    @EventHandler
    public void onPlayerBreakBlockEvent(BlockBreakEvent e) {
        Location l = e.getBlock().getLocation();
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);

        int instruction = builder.createString("MinecraftBlockBreak");
        int worldName = builder.createString(e.getBlock().getWorld().getName());
        int blockdata = builder.createString(e.getBlock().getBlockData().getAsString());

        int[] paramsArray = {blockdata};
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

        NoRepeatBlockBreak.sendInitBlockBreakMessage(e);
    }

    @EventHandler
    public void onPlayerPlaceBlockEvent(BlockPlaceEvent e) {
        final String[] relatives = {
                e.getBlock().getRelative(1, 0, 0).getBlockData().getAsString(),
                e.getBlock().getRelative(-1, 0, 0).getBlockData().getAsString(),
                e.getBlock().getRelative(0, 1, 0).getBlockData().getAsString(),
                e.getBlock().getRelative(0, -1, 0).getBlockData().getAsString(),
                e.getBlock().getRelative(0, 0, 1).getBlockData().getAsString(),
                e.getBlock().getRelative(0, 0, -1).getBlockData().getAsString(),
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
                String[] newRelatives = {
                        e.getBlock().getRelative(1, 0, 0).getBlockData().getAsString(),
                        e.getBlock().getRelative(-1, 0, 0).getBlockData().getAsString(),
                        e.getBlock().getRelative(0, 1, 0).getBlockData().getAsString(),
                        e.getBlock().getRelative(0, -1, 0).getBlockData().getAsString(),
                        e.getBlock().getRelative(0, 0, 1).getBlockData().getAsString(),
                        e.getBlock().getRelative(0, 0, -1).getBlockData().getAsString(),
                };
                for (int i = 0; i < 6; i++) {
                    // seems like the 1.17.1 update made this entire "wait 3 ticks" trick worthless. door adjacent blocks
                    // update before the event handler is called.
                    if (!relatives[i].equals(newRelatives[i]) ||
                            (newRelatives[i].contains("door") && newRelatives[i].contains("half=upper"))
                    ) {
                        Location l = relativeLocations[i];
                        FlatBufferBuilder builder = new FlatBufferBuilder(1024);

                        int instruction = builder.createString("MinecraftBlockPlace");
                        int blockData = builder.createString(newRelatives[i]);
                        int worldName = builder.createString(e.getBlock().getWorld().getName());

                        int[] paramsArray = {blockData};
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
                }
            }
        }.runTaskLater(WorldQLClient.getPluginInstance(), 3);


        Location l = e.getBlock().getLocation();
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);

        int instruction = builder.createString("MinecraftBlockPlace");
        int blockData = builder.createString(e.getBlock().getBlockData().getAsString());
        int worldName = builder.createString(e.getBlock().getWorld().getName());

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
    }

    public static int createRoundedVec3(FlatBufferBuilder builder, double x, double y, double z) {
        return Vec3.createVec3(builder, Math.round(x), Math.round(y), Math.round(z));
    }
}
