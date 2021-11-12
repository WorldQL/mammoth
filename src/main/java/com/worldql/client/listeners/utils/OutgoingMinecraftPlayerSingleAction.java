package com.worldql.client.listeners.utils;

import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.Messages.Instruction;
import com.worldql.client.Messages.Message;
import com.worldql.client.Messages.Vec3d;
import com.worldql.client.WorldQLClient;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import zmq.ZMQ;

import java.nio.ByteBuffer;

public class OutgoingMinecraftPlayerSingleAction {
    public static void sendPacket(Location playerLocation, Player player, String action) {
        FlexBuffersBuilder b = new FlexBuffersBuilder();
        int pmap = b.startMap();
        b.putString("action", action);
        b.putString("username", player.getName());
        b.putString("uuid", player.getUniqueId().toString());
        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();

        FlatBufferBuilder builder = new FlatBufferBuilder(1024);

        int sender_uuid = builder.createString(WorldQLClient.worldQLClientId);
        int worldName = builder.createString(player.getWorld().getName());
        int command = builder.createString("MinecraftPlayerSingleAction");
        int flex = builder.createByteVector(bb);

        Message.startMessage(builder);
        // Inform the other servers about this player movement with a LocalMessage
        Message.addInstruction(builder, Instruction.LocalMessage);
        Message.addWorldName(builder, worldName);
        // Store the "MinecraftPlayerMove" command in the parameter field
        Message.addParameter(builder, command);
        Message.addSenderUuid(builder, sender_uuid);
        Message.addPosition(builder, Vec3d.createVec3d(builder, (float) playerLocation.getX(), (float) playerLocation.getY(), (float) playerLocation.getZ()));
        Message.addFlex(builder, flex);

        int message = Message.endMessage(builder);
        builder.finish(message);

        byte[] buf = builder.sizedByteArray();
        WorldQLClient.getPluginInstance().getPushSocket().send(buf, ZMQ.ZMQ_DONTWAIT);
    }
}
