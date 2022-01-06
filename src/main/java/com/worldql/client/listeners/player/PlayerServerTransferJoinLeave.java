package com.worldql.client.listeners.player;


import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.CrossDirection;
import com.worldql.client.Slices;
import com.worldql.client.WorldQLClient;
import com.worldql.client.ghost.PlayerGhostManager;
import com.worldql.client.minecraft_serialization.SaveLoadPlayerFromRedis;
import com.worldql.client.protocols.ProtocolManager;
import com.worldql.client.worldql_serialization.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import redis.clients.jedis.Jedis;
import zmq.ZMQ;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PlayerServerTransferJoinLeave implements Listener {
    @EventHandler
    public void onPlayerLogOut(PlayerQuitEvent e) {
        SaveLoadPlayerFromRedis.savePlayerToRedis(e.getPlayer());

        if (WorldQLClient.getPluginInstance().inventorySyncOnly) {
            return;
        }

        if (ProtocolManager.isinjected(e.getPlayer()))
            ProtocolManager.uninjectPlayer(e.getPlayer());
        // Send quit event to other clients
        FlexBuffersBuilder b = Codec.getFlexBuilder();
        int pmap = b.startMap();
        b.putString("username", e.getPlayer().getName());
        b.putString("uuid", e.getPlayer().getUniqueId().toString());
        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();
        Message message = new Message(
                Instruction.LocalMessage,
                WorldQLClient.worldQLClientId,
                e.getPlayer().getWorld().getName(),
                Replication.ExceptSelf,
                new Vec3D(e.getPlayer().getLocation()),
                null,
                null,
                "MinecraftPlayerQuit",
                bb
        );
        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
    }

    @EventHandler
    public void onPlayerLogIn(PlayerJoinEvent e) {
        // TODO: Move the IO to async event.
        WorldQLClient.getPluginInstance().playerDataSavingManager.markUnsafe(e.getPlayer());
        Bukkit.getScheduler().runTaskLater(WorldQLClient.getPluginInstance(), new Runnable() {
            @Override
            public void run() {
                // make sure the transferring server doesn't save any junk on the way out.
                WorldQLClient.getPluginInstance().playerDataSavingManager.markSave(e.getPlayer());
                Jedis j = WorldQLClient.pool.getResource();
                String data = j.get("player-" + e.getPlayer().getUniqueId());

                if (data != null) {
                    try {
                        SaveLoadPlayerFromRedis.setPlayerData(data, e.getPlayer());
                        WorldQLClient.getPluginInstance().playerDataSavingManager.markSafe(e.getPlayer());
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }

                if (WorldQLClient.getPluginInstance().inventorySyncOnly) {
                    return;
                }

                Location playerLocation = e.getPlayer().getLocation();
                int locationOwner = Slices.getOwnerOfLocation(playerLocation);

                if (locationOwner != WorldQLClient.mammothServerId) {
                    String cooldownKey = "cooldown-" + e.getPlayer().getUniqueId();

                    if (j.exists(cooldownKey)) {
                        e.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                new TextComponent(ChatColor.GOLD + "You must wait before crossing server borders again!"));
                        CrossDirection shoveDirection = Slices.getShoveDirection(playerLocation);
                        switch (shoveDirection) {
                            case EAST_POSITIVE_X -> e.getPlayer().teleport(playerLocation.clone().add(0.3, 0, 0));
                            case WEST_NEGATIVE_X -> e.getPlayer().teleport(playerLocation.clone().add(-0.3, 0, 0));
                            case NORTH_NEGATIVE_Z -> e.getPlayer().teleport(playerLocation.clone().add(0, 0, -0.3));
                            case SOUTH_POSITIVE_Z -> e.getPlayer().teleport(playerLocation.clone().add(0, 0, 0.3));
                            case ERROR -> {
                                e.getPlayer().kickPlayer("The Mammoth server responsible for your region of the world is inaccessible.");
                            }
                        }
                        WorldQLClient.pool.returnResource(j);
                        return;
                    }

                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("Connect");
                    out.writeUTF("mammoth_" + locationOwner);
                    e.getPlayer().sendPluginMessage(WorldQLClient.getPluginInstance(), "BungeeCord", out.toByteArray());

                    j.set(cooldownKey, "true");
                    j.expire(cooldownKey, 5);

                    WorldQLClient.pool.returnResource(j);
                    return;
                }
                WorldQLClient.pool.returnResource(j);
            }
        }, 35L);


        if (WorldQLClient.getPluginInstance().inventorySyncOnly) {
            return;
        }

        //WorldQLClient.logger.info("Setting player " + e.getPlayer().getDisplayName() + " to get ghost join packets sent.");
        ProtocolManager.injectPlayer(e.getPlayer());
        Player player = e.getPlayer();

        PlayerGhostManager.ensurePlayerHasJoinPackets(player.getUniqueId());

        FlexBuffersBuilder b = Codec.getFlexBuilder();
        int pmap = b.startMap();
        b.putFloat("pitch", player.getLocation().getPitch());
        b.putFloat("yaw", player.getLocation().getYaw());
        b.putString("username", player.getName());
        b.putString("uuid", player.getUniqueId().toString());
        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();

        Message message = new Message(
                Instruction.LocalMessage,
                WorldQLClient.worldQLClientId,
                e.getPlayer().getWorld().getName(),
                Replication.ExceptSelf,
                new Vec3D(player.getLocation()),
                null,
                null,
                "MinecraftPlayerMove",
                bb
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
    }


}
