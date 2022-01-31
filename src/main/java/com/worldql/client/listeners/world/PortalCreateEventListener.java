package com.worldql.client.listeners.world;

import com.worldql.client.WorldQLClient;
import com.worldql.client.listeners.utils.BlockTools;
import com.worldql.client.worldql_serialization.*;
import com.worldql.client.worldql_serialization.Record;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.scheduler.BukkitRunnable;
import zmq.ZMQ;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PortalCreateEventListener implements Listener {
    @EventHandler
    public void onPortalCreate(PortalCreateEvent e) {
        if (e.getBlocks().size() < 1) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                List<Record> portalBlocks = new ArrayList<>();
                for (BlockState b : e.getBlocks()) {
                    if (b.getBlockData().getMaterial().equals(Material.OBSIDIAN) || b.getBlockData().getMaterial().equals(Material.NETHER_PORTAL)) {
                        portalBlocks.add(BlockTools.serializeBlock(b.getBlock()));
                    }
                }
                Location l = e.getBlocks().get(0).getLocation();

                // Create a WorldQL Message containing the broken blocks.
                Message message = new Message(
                        // RecordCreate = Permanently record this change to the world.
                        Instruction.GlobalMessage,
                        WorldQLClient.worldQLClientId,
                        "@global",
                        Replication.ExceptSelf,
                        new Vec3D(l),
                        portalBlocks,
                        null,
                        "MinecraftBlockUpdate",
                        null
                );
                // Get a copy of the message we just sent, but with the Instruction type LocalMessage.
                // This Message notifies other Minecraft servers of the block change immediately (if they are subscribed to the region)
                // Record changes aren't loaded until the chunk is loaded.
                WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
            }
        }.runTaskLater(WorldQLClient.pluginInstance, 1);


    }
}
