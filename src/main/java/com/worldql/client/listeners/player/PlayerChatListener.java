package com.worldql.client.listeners.player;

import com.google.flatbuffers.FlexBuffers;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.worldql_serialization.*;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;
import zmq.ZMQ;

import java.nio.ByteBuffer;
import java.text.MessageFormat;

public class PlayerChatListener implements Listener {
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        if (WorldQLClient.enableChatRelay) {
            // Send chat message to other clients
            FlexBuffersBuilder b = Codec.getFlexBuilder();
            int pmap = b.startMap();
            b.putString("username", e.getPlayer().getName());
            b.putString("message", e.getMessage());
            b.endMap(null, pmap);
            ByteBuffer bb = b.finish();

            Message message = new Message(
                    Instruction.GlobalMessage,
                    WorldQLClient.worldQLClientId,
                    "@global",
                    Replication.ExceptSelf,
                    null,
                    null,
                    null,
                    "MinecraftPlayerChat",
                    bb
            );

            WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
        }
    }

    public static void relayChat(@NotNull Message message) {
        FlexBuffers.Map map = FlexBuffers.getRoot(message.flex()).asMap();

        String playerName = map.get("username").asString();
        String messageText = map.get("message").asString();

        // TODO: Allow configuring custom message format
        String output = MessageFormat.format("{0} §8»§f {1}", playerName, messageText);
        WorldQLClient.getPluginInstance().getServer().broadcastMessage(output);
    }
}
