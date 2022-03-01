package com.worldql.mammoth.listeners.player;

import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.mammoth.MammothPlugin;
import com.worldql.mammoth.worldql_serialization.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import zmq.ZMQ;

import java.nio.ByteBuffer;

public class PlayerTeleportEventListener implements Listener {

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        if (e.getTo() == null) return;
        if (MammothPlugin.processGhosts) {
            FlexBuffersBuilder b = Codec.getFlexBuilder();
            int pmap = b.startMap();
            b.putFloat("pitch", e.getTo().getPitch());
            b.putFloat("yaw", e.getTo().getYaw());
            b.putString("username", e.getPlayer().getName());
            b.putString("uuid", e.getPlayer().getUniqueId().toString());
            b.endMap(null, pmap);
            ByteBuffer bb = b.finish();

            Message message = new Message(
                    Instruction.LocalMessage,
                    MammothPlugin.worldQLClientId,
                    e.getPlayer().getWorld().getName(),
                    Replication.ExceptSelf,
                    new Vec3D(e.getTo()),
                    null,
                    null,
                    "MinecraftPlayerMove",
                    bb
            );

            MammothPlugin.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
        }
    }
}
