package com.worldql.client.listeners;

import WorldQLFB_OLD.StandardEvents.Vec3;
import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.serialization.Instruction;
import com.worldql.client.serialization.Message;
import com.worldql.client.serialization.Vec3D;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import zmq.ZMQ;

public class PlayerBlockPlaceListener implements Listener {
    @EventHandler
    public void onPlayerBreakBlockEvent(BlockBreakEvent e) {
        Message message = new Message(
                Instruction.LocalMessage,
                WorldQLClient.worldQLClientId,
                e.getPlayer().getWorld().getName(),
                new Vec3D(roundLocation(e.getBlock().getLocation())),
                null,
                null,
                "MinecraftBlockBreak",
                null
        );
        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
    }
    private static Location roundLocation(Location l) {
        return new Location(l.getWorld(), Math.round(l.getX()), Math.round(l.getY()), Math.round(l.getZ()));
    }
    public static int createRoundedVec3(FlatBufferBuilder builder, double x, double y, double z) {
        return Vec3.createVec3(builder, Math.round(x), Math.round(y), Math.round(z));
    }
}
