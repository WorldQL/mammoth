package com.worldql.client.listeners.explosions;

import com.worldql.client.WorldQLClient;
import com.worldql.client.serialization.*;
import com.worldql.client.serialization.Record;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import zmq.ZMQ;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EntityExplodeEventListener implements Listener {
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        if (e.getEntity() instanceof TNTPrimed) {
            Entity causer = ((TNTPrimed) e.getEntity()).getSource();
            if (causer == null) {
                // This is a cosmetic primed TNT entity for cross-server visuals.
                e.setCancelled(true);
                e.blockList().clear();
                return;
            }
        }

        // This class is a good example as it utilizes multiple WorldQL patterns and features.
        // Collect all of the blocks broken by this explosion into a list.
        List<Record> brokenBlocks = new ArrayList<>();
        for (Block block : e.blockList()) {
            brokenBlocks.add(new Record(
                    UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes(StandardCharsets.UTF_8)),
                    new Vec3D(block.getLocation()),
                    block.getWorld().getName(),
                    "minecraft:air",
                    null
            ));
        }
        // Create a WorldQL Message containing the broken blocks.
        Message message = new Message(
                // RecordCreate = Permanently record this change to the world.
                Instruction.RecordCreate,
                WorldQLClient.worldQLClientId,
                e.getLocation().getWorld().getName(),
                Replication.ExceptSelf,
                new Vec3D(e.getLocation()),
                brokenBlocks,
                null,
                "MinecraftBlockUpdate",
                null
        );
        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
        // Get a copy of the message we just sent, but with the Instruction type LocalMessage.
        // This Message notifies other Minecraft servers of the block change immediately (if they are subscribed to the region)
        // Record changes aren't loaded until the chunk is loaded.
        Message localMessage = message.withInstruction(Instruction.LocalMessage);
        WorldQLClient.getPluginInstance().getPushSocket().send(localMessage.encode(), ZMQ.ZMQ_DONTWAIT);

        // The actual explosion is sent in ExplosionPrimeEventListener

        // See ZeroMQServer for the class that receives the messages this one creates.

    }
}
