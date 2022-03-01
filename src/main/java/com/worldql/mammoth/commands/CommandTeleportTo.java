package com.worldql.mammoth.commands;

import com.google.flatbuffers.FlexBuffers;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.mammoth.MammothPlugin;
import com.worldql.mammoth.worldql_serialization.Codec;
import com.worldql.mammoth.worldql_serialization.Instruction;
import com.worldql.mammoth.worldql_serialization.Message;
import com.worldql.mammoth.worldql_serialization.Replication;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import zmq.ZMQ;

import java.nio.ByteBuffer;

public class CommandTeleportTo implements CommandExecutor {

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
                MammothPlugin.worldQLClientId,
                "@global",
                Replication.IncludingSelf,
                null,
                null,
                null,
                "MinecraftTeleportPositionLookup",
                bb
        );

        MammothPlugin.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
        return true;
    }

    public static void handlePositionLookup(@NotNull Message incoming) {
        FlexBuffers.Map map = FlexBuffers.getRoot(incoming.flex()).asMap();
        String target = map.get("target").asString();
        String destination = map.get("destination").asString();

        Player destPlayer = Bukkit.getPlayer(destination);
        if (destPlayer != null) {
            Location loc = destPlayer.getLocation();

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

            Message message = new Message(
                    Instruction.GlobalMessage,
                    MammothPlugin.worldQLClientId,
                    "@global",
                    Replication.IncludingSelf,
                    null,
                    null,
                    null,
                    "MinecraftTeleport",
                    bb
            );

            MammothPlugin.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
        }
    }
}
