package com.worldql.mammoth.listeners.utils;

import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.mammoth.MammothPlugin;
import com.worldql.mammoth.worldql_serialization.*;
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
                MammothPlugin.worldQLClientId,
                player.getWorld().getName(),
                Replication.ExceptSelf,
                new Vec3D(playerLocation),
                null,
                null,
                "MinecraftPlayerSingleAction",
                bb
        );

        MammothPlugin.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
    }
    // Used for placing entities like end crystals.
    public static void sendPlaceEndCrystalPacket(World world, Location targetLocation) {
        Message message = new Message(
                Instruction.LocalMessage,
                MammothPlugin.worldQLClientId,
                world.getName(),
                Replication.ExceptSelf,
                new Vec3D(targetLocation),
                null,
                null,
                "MinecraftEndCrystalCreate",
                null
        );

        MammothPlugin.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
    }
}
