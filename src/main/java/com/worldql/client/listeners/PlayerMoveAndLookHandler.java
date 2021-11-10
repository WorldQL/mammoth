package com.worldql.client.listeners;

import WorldQLFB_OLD.StandardEvents.Update;
import WorldQLFB_OLD.StandardEvents.Vec3;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.Messages.Instruction;
import com.worldql.client.Messages.Message;
import com.worldql.client.Messages.Vec3d;

import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.WorldQLClient;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import WorldQLFB_OLD.StandardEvents.*;
import zmq.ZMQ;

import java.nio.ByteBuffer;

public class PlayerMoveAndLookHandler implements Listener {


    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent e) {
        if (e.getTo() == null) return;

        // Encode the actual player information using a Flexbuffer.
        FlexBuffersBuilder b = new FlexBuffersBuilder();
        int pmap = b.startMap();
        b.putFloat("pitch", e.getTo().getPitch());
        b.putFloat("yaw", e.getTo().getYaw());
        b.putString("username", e.getPlayer().getName());
        b.putString("uuid", e.getPlayer().getUniqueId().toString());
        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();

        FlatBufferBuilder builder = new FlatBufferBuilder(1024);

        int uuid = builder.createString(e.getPlayer().getUniqueId().toString());
        int name = builder.createString(e.getPlayer().getName());
        int sender_uuid = builder.createString(WorldQLClient.worldQLClientId);
        int worldName = builder.createString(e.getPlayer().getWorld().getName());
        int command = builder.createString("MinecraftPlayerMove");
        int flex = builder.createByteVector(bb);

        Message.startMessage(builder);
        // Inform the other servers about this player movement with a LocalMessage
        Message.addInstruction(builder, Instruction.LocalMessage);
        Message.addWorldName(builder, worldName);
        // Store the "MinecraftPlayerMove" command in the parameter field
        Message.addParameter(builder, command);
        Message.addSenderUuid(builder, sender_uuid);
        Message.addPosition(builder, Vec3d.createVec3d(builder, (float) e.getTo().getX(), (float) e.getTo().getY(), (float) e.getTo().getZ()));
        Message.addFlex(builder, flex);

        int message = Message.endMessage(builder);
        builder.finish(message);

        byte[] buf = builder.sizedByteArray();
        WorldQLClient.getPluginInstance().getPushSocket().send(buf, ZMQ.ZMQ_DONTWAIT);
    }
}
