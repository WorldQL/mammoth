package com.worldql.client.listeners.utils;

import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.MessageCodec;
import com.worldql.client.Messages.Instruction;
import com.worldql.client.WorldQLClient;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import zmq.ZMQ;

import java.nio.ByteBuffer;

public class OutgoingMinecraftPlayerSingleAction {
    public static void sendPacket(Location playerLocation, Player player, String action) {
        FlexBuffersBuilder b = MessageCodec.getFlexBuilder();
        int pmap = b.startMap();
        b.putString("action", action);
        b.putString("username", player.getName());
        b.putString("uuid", player.getUniqueId().toString());
        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();

        MessageCodec.Vec3D position = new MessageCodec.Vec3D((float) playerLocation.getX(), (float) playerLocation.getY(), (float) playerLocation.getZ());
        byte[] buf = MessageCodec.encodeMessage(
                WorldQLClient.worldQLClientId,
                Instruction.LocalMessage,
                player.getWorld().getName(),
                position,
                "MinecraftPlayerSingleAction",
                bb
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(buf, ZMQ.ZMQ_DONTWAIT);
    }
}
