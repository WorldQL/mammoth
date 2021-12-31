package com.worldql.client.listeners.player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.flatbuffers.FlexBuffersBuilder;

import com.worldql.client.CrossDirection;
import com.worldql.client.Slices;
import com.worldql.client.WorldQLClient;
import com.worldql.client.worldql_serialization.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import redis.clients.jedis.Jedis;
import zmq.ZMQ;

import java.nio.ByteBuffer;

public class PlayerMoveAndLookHandler implements Listener {
    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent e) {
        Location playerLocation = e.getTo();
        int locationOwner = Slices.getOwnerOfLocation(playerLocation);


        // 1. Compute the "edge direction" defined by the direction from the source server TO the destination server.
        // 2. If the user is on cooldown, push them back one block in the direction they came from.

        if (locationOwner != WorldQLClient.mammothServerId) {
            Jedis j = WorldQLClient.pool.getResource();
            String cooldownKey = "cooldown-" + e.getPlayer().getUniqueId();

            if (j.exists(cooldownKey)) {
                e.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.GOLD + "You must wait before crossing server borders again!"));
                CrossDirection shoveDirection = Slices.getShoveDirection(playerLocation);

                switch (shoveDirection) {
                    case EAST_POSITIVE_X -> e.getPlayer().teleport(playerLocation.clone().add(0.3, 0,0));
                    case WEST_NEGATIVE_X -> e.getPlayer().teleport(playerLocation.clone().add(-0.3, 0, 0));
                    case NORTH_NEGATIVE_Z -> e.getPlayer().teleport(playerLocation.clone().add(0, 0, -0.3));
                    case SOUTH_POSITIVE_Z -> e.getPlayer().teleport(playerLocation.clone().add(0, 0, 0.3));
                    case ERROR -> {
                        e.getPlayer().kickPlayer("The Mammoth server responsible for your region of the world is inaccessible.");
                        return;
                    }
                }

                WorldQLClient.pool.returnResource(j);
                return;
            }

            PlayerServerTransferJoinLeave.savePlayerToRedis(e.getPlayer());
            ByteArrayDataOutput out = ByteStreams.newDataOutput();

            out.writeUTF("Connect");
            out.writeUTF("mammoth_" + locationOwner);
            e.getPlayer().sendPluginMessage(WorldQLClient.getPluginInstance(), "BungeeCord", out.toByteArray());

            j.set(cooldownKey, "true");
            j.expire(cooldownKey, 10);

            WorldQLClient.pool.returnResource(j);
            return;
        }

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
                Replication.ExceptSelf,
                new Vec3D(e.getTo()),
                null,
                null,
                "MinecraftPlayerMove",
                bb
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
    }
}
