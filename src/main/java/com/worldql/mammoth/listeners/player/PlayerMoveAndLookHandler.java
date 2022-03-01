package com.worldql.mammoth.listeners.player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.mammoth.CrossDirection;
import com.worldql.mammoth.Slices;
import com.worldql.mammoth.MammothPlugin;
import com.worldql.mammoth.minecraft_serialization.SaveLoadPlayerFromRedis;
import com.worldql.mammoth.worldql_serialization.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import zmq.ZMQ;

import java.nio.ByteBuffer;

public class PlayerMoveAndLookHandler implements Listener {
    private void setVelocityOfPlayerOrRiddenEntity(Player p, Vector v) {
        if (p.isInsideVehicle()) {
            p.getVehicle().setVelocity(v);
        } else {
            p.setVelocity(v);
        }
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent e) {
        if (!MammothPlugin.playerDataSavingManager.isFullySynced(e.getPlayer())) {
            if (MammothPlugin.playerDataSavingManager.getMsSinceLogin(e.getPlayer()) > 2000) {
                e.getPlayer().spigot().sendMessage(ChatMessageType.CHAT,
                        new TextComponent(ChatColor.DARK_RED + "It's taking longer than expected to load your player data. " + ChatColor.DARK_AQUA + "Please try re-logging. If you're seeing this error often, notify the server admins."));
            }
            e.setCancelled(true);
            return;
        }

        Location playerLocation = e.getTo();
        int locationOwner = Slices.getOwnerOfLocation(playerLocation);

        if (Slices.isDMZ(playerLocation)) {
            int distance = Slices.getDistanceFromSliceBoundary(playerLocation);
            if (distance > 1) {
                e.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.RED + "You are " + ChatColor.BOLD + "" + distance + ChatColor.RESET + ChatColor.RED + " blocks away from a server border."));
            }
        }

        if (locationOwner != MammothPlugin.mammothServerId) {
            if (MammothPlugin.playerDataSavingManager.getMsSinceLogin(e.getPlayer()) < 5000) {
                e.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.GOLD + "" + ChatColor.BOLD + "(!) You must wait 5 seconds between crossing server borders!"));

                // 1. Compute the "cross direction" defined by the direction from the source server TO the destination server.
                // 2. Push them back in the direction they came from towards the correct server.
                CrossDirection shoveDirection = Slices.getShoveDirection(playerLocation);
                Vector v = new Vector(0, 0, 0);
                switch (shoveDirection) {
                    case EAST_POSITIVE_X -> {
                        v = new Vector(.2, 0, 0);
                    }
                    case WEST_NEGATIVE_X -> {
                        v = new Vector(-.2, 0, 0);
                    }
                    case NORTH_NEGATIVE_Z -> {
                        v = new Vector(0, 0, -.2);
                    }
                    case SOUTH_POSITIVE_Z -> {
                        v = new Vector(0, 0, .2);
                    }
                    case ERROR -> {
                        if (MammothPlugin.playerDataSavingManager.getMsSinceLogin(e.getPlayer()) > 4000) {
                            e.getPlayer().kickPlayer("The Mammoth server responsible for your region of the world is inaccessible.");
                        }
                    }
                }
                setVelocityOfPlayerOrRiddenEntity(e.getPlayer(), v);
                e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 20, .5F);
                return;
            }

            SaveLoadPlayerFromRedis.saveLeavingPlayerToRedisAsync(e.getPlayer(), true);

            Bukkit.getScheduler().runTaskLaterAsynchronously(MammothPlugin.getPluginInstance(), () -> {
                if (!MammothPlugin.playerDataSavingManager.hasBeenTransferred(e.getPlayer())) {
                    MammothPlugin.playerDataSavingManager.markTransferred(e.getPlayer());
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("Connect");
                    out.writeUTF(MammothPlugin.serverPrefix + locationOwner);
                    e.getPlayer().sendPluginMessage(MammothPlugin.getPluginInstance(), "BungeeCord", out.toByteArray());
                }
            }, 2L);

            return;
        }

        if (e.getTo() == null) return;

        if (!MammothPlugin.processGhosts) {
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
