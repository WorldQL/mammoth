package com.worldql.client.listeners;

import WorldQLFB.StandardEvents.Update;
import WorldQLFB.StandardEvents.Vec3;
import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.WorldQLClient;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;

public class PlayerBlockPlaceListener implements Listener {
    @EventHandler
    public void onPlayerBreakBlockEvent(BlockBreakEvent e) {
        Location l = e.getBlock().getLocation();
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);
        int instruction = builder.createString("MinecraftBlockBreak");
        int worldName = builder.createString(e.getBlock().getWorld().getName());
        int blockdata = builder.createString(e.getBlock().getBlockData().getAsString());
        int[] params_array = {blockdata};
        int params = Update.createParamsVector(builder, params_array);
        Update.startUpdate(builder);
        Update.addInstruction(builder, instruction);
        Update.addWorldName(builder, worldName);
        Update.addPosition(builder, createRoundedVec3(builder, l.getX(), l.getY(), l.getZ()));
        Update.addParams(builder, params);
        Update.addSenderid(builder, WorldQLClient.zmqPortClientId);
        int blockupdate = Update.endUpdate(builder);
        builder.finish(blockupdate);

        byte[] buf = builder.sizedByteArray();
        WorldQLClient.push_socket.send(buf, 0);
    }

    @EventHandler
    public void onPlayerPlaceBlockEvent(BlockPlaceEvent e) {
        String[] relatives = {
                e.getBlock().getRelative(1, 0, 0).getBlockData().getAsString(),
                e.getBlock().getRelative(-1, 0, 0).getBlockData().getAsString(),
                e.getBlock().getRelative(0, 1, 0).getBlockData().getAsString(),
                e.getBlock().getRelative(0, -1, 0).getBlockData().getAsString(),
                e.getBlock().getRelative(0, 0, 1).getBlockData().getAsString(),
                e.getBlock().getRelative(0, 0, -1).getBlockData().getAsString(),
        };
        Location[] relative_locations = {
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
                    if (!relatives[i].equals(newRelatives[i])) {
                        Location l = relative_locations[i];
                        FlatBufferBuilder builder = new FlatBufferBuilder(1024);
                        int instruction = builder.createString("MinecraftBlockPlace");
                        int blockdata = builder.createString(newRelatives[i]);
                        int worldName = builder.createString(e.getBlock().getWorld().getName());
                        int[] params_array = {blockdata};
                        int params = Update.createParamsVector(builder, params_array);
                        Update.startUpdate(builder);
                        Update.addInstruction(builder, instruction);
                        Update.addWorldName(builder, worldName);
                        Update.addPosition(builder, createRoundedVec3(builder, l.getX(), l.getY(), l.getZ()));
                        Update.addParams(builder, params);
                        Update.addSenderid(builder, WorldQLClient.zmqPortClientId);
                        int blockupdate = Update.endUpdate(builder);
                        builder.finish(blockupdate);

                        byte[] buf = builder.sizedByteArray();
                        WorldQLClient.push_socket.send(buf, 0);
                    }
                }
            }
        }.runTaskLater(WorldQLClient.plugin_instance, 3);


        Location l = e.getBlock().getLocation();
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);
        int instruction = builder.createString("MinecraftBlockPlace");
        int blockdata = builder.createString(e.getBlock().getBlockData().getAsString());
        int worldName = builder.createString(e.getBlock().getWorld().getName());
        int[] params_array = {blockdata};
        int params = Update.createParamsVector(builder, params_array);
        Update.startUpdate(builder);
        Update.addInstruction(builder, instruction);
        Update.addWorldName(builder, worldName);
        Update.addPosition(builder, createRoundedVec3(builder, l.getX(), l.getY(), l.getZ()));
        Update.addParams(builder, params);
        Update.addSenderid(builder, WorldQLClient.zmqPortClientId);
        int blockupdate = Update.endUpdate(builder);
        builder.finish(blockupdate);


        byte[] buf = builder.sizedByteArray();
        WorldQLClient.push_socket.send(buf, 0);
    }

    public static int createRoundedVec3(FlatBufferBuilder builder, double x, double y, double z) {
        return Vec3.createVec3(builder, Math.round(x), Math.round(y), Math.round(z));
    }
}
