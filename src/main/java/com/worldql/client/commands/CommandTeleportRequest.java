package com.worldql.client.commands;

import com.google.flatbuffers.FlexBuffers;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.worldql_serialization.Codec;
import com.worldql.client.worldql_serialization.Instruction;
import com.worldql.client.worldql_serialization.Message;
import com.worldql.client.worldql_serialization.Replication;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import zmq.ZMQ;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.UUID;

// Takes a single argument: the destination player we want to teleport to.
public class CommandTeleportRequest implements CommandExecutor {
    public static final HashMap<UUID, ByteBuffer> pendingTeleportRequests = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player target)) {
            // Must be used by a player
            return false;
        }

        if (args.length < 1) {
            // Must have a destination player
            return false;
        }

        String destination = args[0];

        FlexBuffersBuilder b = Codec.getFlexBuilder();
        int pmap = b.startMap();

        b.putString("target", target.getUniqueId().toString());
        b.putString("username", target.getName());
        b.putString("destination", destination);
        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();

        Message message = new Message(
                Instruction.GlobalMessage,
                WorldQLClient.worldQLClientId,
                "@global",
                Replication.IncludingSelf,
                null,
                null,
                null,
                "MinecraftTeleportRequest",
                bb
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
        target.sendMessage(ChatColor.GREEN + "Teleport request sent!");
        return true;
    }

    public static void handlePositionLookup(@NotNull Message incoming) {
        FlexBuffers.Map map = FlexBuffers.getRoot(incoming.flex()).asMap();
        String target = map.get("target").asString();
        String destination = map.get("destination").asString();

        String username = map.get("username").asString();

        Player destPlayer = Bukkit.getPlayer(destination);
        if (destPlayer != null) {
            Location loc = destPlayer.getLocation();

            destPlayer.sendMessage(ChatColor.AQUA + "Incoming teleport request from " + username + ". Run /mtpaccept to accept.");

            FlexBuffersBuilder b = Codec.getFlexBuilder();
            int pmap = b.startMap();

            b.putString("target", target);
            b.putString("world", loc.getWorld().getName());
            b.putFloat("x", loc.getX());
            b.putFloat("y", loc.getY());
            b.putFloat("z", loc.getZ());
            b.putFloat("pitch", loc.getPitch());
            b.putFloat("yaw", loc.getYaw());
            b.endMap(null, pmap);
            ByteBuffer bb = b.finish();

            pendingTeleportRequests.put(destPlayer.getUniqueId(), bb);
        }
    }

    public static void handleTeleport(@NotNull Message incoming) {
        FlexBuffers.Map map = FlexBuffers.getRoot(incoming.flex()).asMap();
        String target = map.get("target").asString();
        UUID targetID = UUID.fromString(target);

        Player player = Bukkit.getPlayer(targetID);

        if (player != null) {
            double x = map.get("x").asFloat();
            double y = map.get("y").asFloat();
            double z = map.get("z").asFloat();
            String worldName = map.get("world").asString();

            World world = Bukkit.getWorld(worldName);
            Location location = new Location(world, x, y, z);

            Bukkit.getScheduler().runTask(WorldQLClient.getPluginInstance(), () -> {
                player.teleport(location);
            });
        }
    }
}
