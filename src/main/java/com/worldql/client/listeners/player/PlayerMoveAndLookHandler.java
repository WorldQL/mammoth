package com.worldql.client.listeners.player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.flatbuffers.FlexBuffersBuilder;

import com.worldql.client.Slices;
import com.worldql.client.WorldQLClient;
import com.worldql.client.worldql_serialization.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import zmq.ZMQ;

import java.nio.ByteBuffer;

public class PlayerMoveAndLookHandler implements Listener {
    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent e) {
        Location playerLocation = e.getPlayer().getLocation();
        int locationOwner = Slices.getOwnerOfLocation(playerLocation);
        String sliceMessage = "Server: " + locationOwner;
        if (Slices.isDMZ(e.getPlayer().getLocation())) {
            sliceMessage += " " + ChatColor.RED + "In cross-server zone.";
        }
        e.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(sliceMessage));

        if (locationOwner != WorldQLClient.mammothServerId) {
            System.out.println("MISMATCH");
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF("mammoth_" + locationOwner);
            e.getPlayer().sendPluginMessage(WorldQLClient.getPluginInstance(), "BungeeCord", out.toByteArray());
            return;
        }


        if (e.getTo() == null) return;

        // Encode the actual player information using a Flexbuffer.
        // Represents the following JSON object (numerical values are just examples
        // { pitch: 25.3, yaw: 34.2, username: "test", "uuid": "5e34a615-a7ac-4bd8-a039-9c0df1b1b5ec" }
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
