package com.worldql.client.listeners;

import WorldQLFB_OLD.StandardEvents.Update;
import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.events.OutgoingPlayerHitEvent;
import com.worldql.client.ghost.ExpiringEntityPlayer;
import com.worldql.client.ghost.PlayerGhostManager;
import com.worldql.client.serialization.*;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import zmq.ZMQ;

import java.nio.ByteBuffer;

public class OutgoingPlayerHitListener implements Listener {

    @EventHandler
    public void onPlayerHit(OutgoingPlayerHitEvent event) {
        FlexBuffersBuilder b = Codec.getFlexBuilder();
        int pmap = b.startMap();

        //b.putFloat("damage", event.getDamage());
        b.putString("username", event.getReceiver().getName());
        b.putString("uuid", event.getUUID().toString());
        b.putString("uuidofattacker", event.getAttacker().getUniqueId().toString());
        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();

        Message message = new Message(
                Instruction.LocalMessage,
                WorldQLClient.worldQLClientId,
                event.getAttacker().getWorld().getName(),
                Replication.ExceptSelf,
                new Vec3D(new Location(event.getAttacker().getWorld(),
                        event.getReceiver().locX(), event.getReceiver().locY(), event.getReceiver().locZ())),
                null,
                null,
                "MinecraftPlayerDamage",
                bb
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);

    }
}
