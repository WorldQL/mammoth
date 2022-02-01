package com.worldql.client.listeners.player;


import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.Slices;
import com.worldql.client.WorldQLClient;
import com.worldql.client.ghost.PlayerGhostManager;
import com.worldql.client.minecraft_serialization.SaveLoadPlayerFromRedis;
import com.worldql.client.protocols.ProtocolManager;
import com.worldql.client.worldql_serialization.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
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
        SaveLoadPlayerFromRedis.saveLeavingPlayerToRedisAsync(e.getPlayer(), false);
        if (WorldQLClient.processGhosts) {
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
    }

    @EventHandler
    public void onPlayerLogIn(PlayerJoinEvent e) {
        WorldQLClient.playerDataSavingManager.markUnsynced(e.getPlayer());
        WorldQLClient.playerDataSavingManager.recordLogin(e.getPlayer());

        Bukkit.getScheduler().runTaskLaterAsynchronously(WorldQLClient.getPluginInstance(), () -> {
            // make sure the transferring server doesn't save any junk on the way out.
            WorldQLClient.playerDataSavingManager.markSavedForDebounce(e.getPlayer());
            String data;
            try (Jedis j = WorldQLClient.pool.getResource()) {
                data = j.get("player-" + e.getPlayer().getUniqueId());
            }

            Bukkit.getScheduler().runTask(WorldQLClient.getPluginInstance(), () -> {
                if (data != null) {
                    if (data.equals("dead")) {
                        // TODO: Respawn the player naturally as if they never connected. This will fix the issues DonutSMP has been having
                        // due to the death kick/ban plugin.
                        e.getPlayer().teleport(Bukkit.getWorld(WorldQLClient.worldName).getSpawnLocation());
                        WorldQLClient.playerDataSavingManager.markSynced(e.getPlayer());
                        e.getPlayer().spigot().respawn();
                        return;
                    }
                    try {
                        SaveLoadPlayerFromRedis.setPlayerData(data, e.getPlayer());
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }

                    Location playerLocation = e.getPlayer().getLocation();
                    int locationOwner = Slices.getOwnerOfLocation(playerLocation);

                    if (locationOwner != WorldQLClient.mammothServerId) {
                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        out.writeUTF("Connect");
                        out.writeUTF("mammoth_" + locationOwner);
                        e.getPlayer().sendPluginMessage(WorldQLClient.getPluginInstance(), "BungeeCord", out.toByteArray());
                    }
                }
                WorldQLClient.playerDataSavingManager.markSynced(e.getPlayer());
            });
        }, 5L);

        //WorldQLClient.logger.info("Setting player " + e.getPlayer().getDisplayName() + " to get ghost join packets sent.");

        if (WorldQLClient.processGhosts) {
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
}
