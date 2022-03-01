package com.worldql.mammoth.listeners.player;

import com.google.flatbuffers.FlexBuffers;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.mammoth.MammothPlugin;
import com.worldql.mammoth.worldql_serialization.*;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;
import zmq.ZMQ;

import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.UUID;

public class PlayerChatListener implements Listener {
    public static String chatFormat;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        if (MammothPlugin.enableChatRelay) {
            // Send chat message to other clients
            FlexBuffersBuilder b = Codec.getFlexBuilder();
            int pmap = b.startMap();
            b.putString("username", e.getPlayer().getName());
            b.putString("message", e.getMessage());
            b.putString("uuid", e.getPlayer().getUniqueId().toString());
            b.endMap(null, pmap);
            ByteBuffer bb = b.finish();

            Message message = new Message(
                    Instruction.GlobalMessage,
                    MammothPlugin.worldQLClientId,
                    "@global",
                    Replication.ExceptSelf,
                    null,
                    null,
                    null,
                    "MinecraftPlayerChat",
                    bb
            );

            MammothPlugin.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
        }
    }

    public static void relayChat(@NotNull Message message) {
        FlexBuffers.Map map = FlexBuffers.getRoot(message.flex()).asMap();

        String playerName = map.get("username").asString();
        Player player = Bukkit.getPlayer(UUID.fromString(map.get("uuid").asString()));
        String messageText = map.get("message").asString();

        String output;
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            output = PlaceholderAPI.setPlaceholders(player, chatFormat);
        } else {
            output = MessageFormat.format(chatFormat, playerName, messageText);
        }
        MammothPlugin.getPluginInstance().getServer().broadcastMessage(output);
    }
}
