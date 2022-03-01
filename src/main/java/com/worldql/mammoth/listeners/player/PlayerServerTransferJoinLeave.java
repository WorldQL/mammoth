package com.worldql.mammoth.listeners.player;


import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.mammoth.Slices;
import com.worldql.mammoth.MammothPlugin;
import com.worldql.mammoth.ghost.PlayerGhostManager;
import com.worldql.mammoth.minecraft_serialization.SaveLoadPlayerFromRedis;
import com.worldql.mammoth.protocols.ProtocolManager;
import com.worldql.mammoth.worldql_serialization.*;
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
        if (MammothPlugin.processGhosts) {
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
                    MammothPlugin.worldQLClientId,
                    e.getPlayer().getWorld().getName(),
                    Replication.ExceptSelf,
                    new Vec3D(e.getPlayer().getLocation()),
                    null,
                    null,
                    "MinecraftPlayerQuit",
                    bb
            );
            MammothPlugin.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
        }
    }

    @EventHandler
    public void onPlayerLogIn(PlayerJoinEvent e) {
        MammothPlugin.playerDataSavingManager.markUnsynced(e.getPlayer());
        MammothPlugin.playerDataSavingManager.recordLogin(e.getPlayer());

        Bukkit.getScheduler().runTaskLaterAsynchronously(MammothPlugin.getPluginInstance(), () -> {
            // make sure the transferring server doesn't save any junk on the way out.
            MammothPlugin.playerDataSavingManager.markSavedForDebounce(e.getPlayer());
            String data;
            try (Jedis j = MammothPlugin.pool.getResource()) {
                data = j.get("player-" + e.getPlayer().getUniqueId());
            }

            Bukkit.getScheduler().runTask(MammothPlugin.getPluginInstance(), () -> {
                if (data != null) {
                    if (data.equals("dead")) {
                        // TODO: Respawn the player naturally as if they never connected. This will fix the issues DonutSMP has been having
                        // due to the death kick/ban plugin.
                        e.getPlayer().teleport(Bukkit.getWorld(MammothPlugin.worldName).getSpawnLocation());
                        MammothPlugin.playerDataSavingManager.markSynced(e.getPlayer());
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

                    if (locationOwner != MammothPlugin.mammothServerId) {
                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        out.writeUTF("Connect");
                        out.writeUTF(MammothPlugin.serverPrefix + locationOwner);
                        e.getPlayer().sendPluginMessage(MammothPlugin.getPluginInstance(), "BungeeCord", out.toByteArray());
                    }
                }
                MammothPlugin.playerDataSavingManager.markSynced(e.getPlayer());
            });
        }, 5L);

        //WorldQLClient.logger.info("Setting player " + e.getPlayer().getDisplayName() + " to get ghost join packets sent.");

        if (MammothPlugin.processGhosts) {
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
                    MammothPlugin.worldQLClientId,
                    e.getPlayer().getWorld().getName(),
                    Replication.ExceptSelf,
                    new Vec3D(player.getLocation()),
                    null,
                    null,
                    "MinecraftPlayerMove",
                    bb
            );

            MammothPlugin.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
        }
    }
}
