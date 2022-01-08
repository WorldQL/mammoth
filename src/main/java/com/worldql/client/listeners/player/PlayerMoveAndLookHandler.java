package com.worldql.client.listeners.player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.flatbuffers.FlexBuffersBuilder;

import com.worldql.client.CrossDirection;
import com.worldql.client.Slices;
import com.worldql.client.WorldQLClient;
import com.worldql.client.minecraft_serialization.SaveLoadPlayerFromRedis;
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
        if (!WorldQLClient.playerDataSavingManager.isSafe(e.getPlayer())) {
            e.setCancelled(true);
            return;
        }

        Location playerLocation = e.getTo();
        int locationOwner = Slices.getOwnerOfLocation(playerLocation);

        if (Slices.isDMZ(playerLocation)) {
            int distance = Slices.getDistanceFromDMZ(playerLocation);
            e.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(ChatColor.RED + "You are " + ChatColor.BOLD + "" + distance + ChatColor.RESET + ChatColor.RED + " blocks away from a server boundary."));
        }

        if (locationOwner != WorldQLClient.mammothServerId) {
            Jedis j = WorldQLClient.pool.getResource();
            String cooldownKey = "cooldown-" + e.getPlayer().getUniqueId();

            if (j.exists(cooldownKey)) {
                e.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.GOLD + "You must wait before crossing server boundaries again!"));

                // 1. Compute the "cross direction" defined by the direction from the source server TO the destination server.
                // 2. Push them back in the direction they came from towards the correct server.
                CrossDirection shoveDirection = Slices.getShoveDirection(playerLocation);
                switch (shoveDirection) {
                    case EAST_POSITIVE_X -> e.getPlayer().teleport(playerLocation.clone().add(0.3, 0,0));
                    case WEST_NEGATIVE_X -> e.getPlayer().teleport(playerLocation.clone().add(-0.3, 0, 0));
                    case NORTH_NEGATIVE_Z -> e.getPlayer().teleport(playerLocation.clone().add(0, 0, -0.3));
                    case SOUTH_POSITIVE_Z -> e.getPlayer().teleport(playerLocation.clone().add(0, 0, 0.3));
                    case ERROR -> e.getPlayer().kickPlayer("The Mammoth server responsible for your region of the world is inaccessible.");
                }

                WorldQLClient.pool.returnResource(j);
                return;
            }

            // TODO: Move the IO to async event.
            SaveLoadPlayerFromRedis.savePlayerToRedisAsync(e.getPlayer());

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF("mammoth_" + locationOwner);
            e.getPlayer().sendPluginMessage(WorldQLClient.getPluginInstance(), "BungeeCord", out.toByteArray());

            j.set(cooldownKey, "true");
            j.expire(cooldownKey, 5);

            WorldQLClient.pool.returnResource(j);
            return;
        }

        if (e.getTo() == null) return;

        if (!WorldQLClient.getPluginInstance().processGhosts) {
            return;
        }

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
