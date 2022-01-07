package com.worldql.client.listeners.explosions;

import com.worldql.client.Slices;
import com.worldql.client.WorldQLClient;
import com.worldql.client.worldql_serialization.*;
import com.worldql.client.worldql_serialization.Record;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import zmq.ZMQ;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class BlockExplodeEventListener implements Listener {
    @EventHandler
    public void onBlockExplodeEvent(BlockExplodeEvent e) {
        if (Slices.enabled && Slices.isDMZ(e.getBlock().getLocation())) {
            e.setCancelled(true);
            UUID blockUuid = UUID.nameUUIDFromBytes(e.getBlock().getLocation().toString().getBytes(StandardCharsets.UTF_8));
            Record airBlock = new Record(
                    blockUuid,
                    new Vec3D(e.getBlock().getLocation()),
                    e.getBlock().getWorld().getName(),
                    "minecraft:air",
                    null
            );
            Message message = new Message(
                    Instruction.RecordCreate,
                    WorldQLClient.worldQLClientId,
                    e.getBlock().getLocation().getWorld().getName(),
                    Replication.ExceptSelf,
                    // This field isn't really used since the Record also contains the position
                    // of the changed block(s).
                    new Vec3D(e.getBlock().getLocation()),
                    List.of(airBlock),
                    null,
                    "MinecraftBlockUpdate",
                    null
            );
            Message globalMessage = message.withInstruction(Instruction.GlobalMessage);
            WorldQLClient.getPluginInstance().getPushSocket().send(globalMessage.encode(), ZMQ.ZMQ_DONTWAIT);
        }
    }
}
