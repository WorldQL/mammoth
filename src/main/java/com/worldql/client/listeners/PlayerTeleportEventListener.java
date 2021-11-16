package com.worldql.client.listeners;

import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.serialization.Codec;
import com.worldql.client.serialization.Instruction;
import com.worldql.client.serialization.Message;
import com.worldql.client.serialization.Vec3D;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import zmq.ZMQ;

import java.nio.ByteBuffer;

public class PlayerTeleportEventListener implements Listener {

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e){
        if (e.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL || e.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            e.setCancelled(true);
            // temporarily disable nether and end portals
            // todo: remove this.
        } else {
            if (e.getTo() == null) return;

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
                    WorldQLClient.worldQLClientId,
                    e.getPlayer().getWorld().getName(),
                    new Vec3D(e.getTo()),
                    null,
                    null,
                    "MinecraftPlayerMove",
                    bb
            );

            WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
        }
    }
}
