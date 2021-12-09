package com.worldql.client.listeners.utils;

import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.serialization.*;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import zmq.ZMQ;

import java.nio.ByteBuffer;

public class OutgoingMinecraftPlayerSingleAction {
    public static void sendPacket(Location playerLocation, Player player, String action) {
        FlexBuffersBuilder b = Codec.getFlexBuilder();
        int pmap = b.startMap();
        b.putString("action", action);
        b.putString("username", player.getName());
        b.putString("uuid", player.getUniqueId().toString());
        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();

        Message message = new Message(
                Instruction.LocalMessage,
                WorldQLClient.worldQLClientId,
                player.getWorld().getName(),
                Replication.ExceptSelf,
                new Vec3D(playerLocation),
                null,
                null,
                "MinecraftPlayerSingleAction",
                bb
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
    }
    // Used for placing entities like end crystals.
    public static void sendPlaceEndCrystalPacket(World world, Location targetLocation) {
        Message message = new Message(
                Instruction.LocalMessage,
                WorldQLClient.worldQLClientId,
                world.getName(),
                Replication.ExceptSelf,
                new Vec3D(targetLocation),
                null,
                null,
                "MinecraftEndCrystalCreate",
                null
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
    }
}
