package com.worldql.client.listeners;

import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.MessageCodec;
import com.worldql.client.Messages.Instruction;

import com.worldql.client.WorldQLClient;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import zmq.ZMQ;

import java.nio.ByteBuffer;

public class PlayerMoveAndLookHandler implements Listener {
    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent e) {
        if (e.getTo() == null) return;

        // Encode the actual player information using a Flexbuffer.
        // Represents the following JSON object (numerical values are just examples
        // { pitch: 25.3, yaw: 34.2, username: "test", "uuid": "5e34a615-a7ac-4bd8-a039-9c0df1b1b5ec" }
        FlexBuffersBuilder b = MessageCodec.getFlexBuilder();
        int pmap = b.startMap();
        b.putFloat("pitch", e.getTo().getPitch());
        b.putFloat("yaw", e.getTo().getYaw());
        b.putString("username", e.getPlayer().getName());
        b.putString("uuid", e.getPlayer().getUniqueId().toString());
        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();

        MessageCodec.Vec3D position = new MessageCodec.Vec3D((float) e.getTo().getX(), (float) e.getTo().getY(), (float) e.getTo().getZ());
        byte[] buf = MessageCodec.encodeMessage(
                WorldQLClient.worldQLClientId,
                Instruction.LocalMessage,
                e.getPlayer().getWorld().getName(),
                position,
                "MinecraftPlayerMove",
                bb
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(buf, ZMQ.ZMQ_DONTWAIT);
    }
}
