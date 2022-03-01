package com.worldql.mammoth.commands;

import com.worldql.mammoth.MammothPlugin;
import com.worldql.mammoth.worldql_serialization.Instruction;
import com.worldql.mammoth.worldql_serialization.Message;
import com.worldql.mammoth.worldql_serialization.Replication;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import zmq.ZMQ;

import java.nio.ByteBuffer;

public class CommandTeleportRequestAccept implements CommandExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player target)) {
            // Must be used by a player
            return false;
        }

        ByteBuffer bb = CommandTeleportRequest.pendingTeleportRequests.get(target.getUniqueId());
        if (bb != null) {
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
            target.sendMessage(ChatColor.GREEN + "Teleport request accepted!");
            return true;
        } else {
            target.sendMessage(ChatColor.RED + "You do not have any pending teleport requests.");
            return false;
        }
    }
}
