package com.worldql.client.listeners.player;

import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.worldql_serialization.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import zmq.ZMQ;

import java.nio.ByteBuffer;

public class PlayerShootBowListener implements Listener {


    private static void pushCommand(Player player, boolean charging) {
        FlexBuffersBuilder b = Codec.getFlexBuilder();
        int pmap = b.startMap();

        b.putBoolean("charging", charging); // true when they are drawing the bow back.
        b.putString("username", player.getName());
        b.putString("uuid", player.getUniqueId().toString());
        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();

        Message message = new Message(
                Instruction.LocalMessage,
                WorldQLClient.worldQLClientId,
                player.getWorld().getName(),
                Replication.ExceptSelf,
                new Vec3D(player.getLocation()),
                null,
                null,
                "MinecraftPlayerShootBow",
                bb
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
    }

    @EventHandler
    public void onPlayerShooting(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;
        pushCommand(player, false);
    }

    @EventHandler
    public void onPlayerShooting(PlayerInteractEvent event) {
        if (!event.hasItem())
            return;
        if (!event.getItem().getType().equals(Material.BOW))
            return;
        if (!event.getAction().name().contains("RIGHT"))
            return;

        pushCommand(event.getPlayer(), true);
    }
}
