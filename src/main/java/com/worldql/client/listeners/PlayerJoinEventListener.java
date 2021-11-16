package com.worldql.client.listeners;

import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.ghost.PlayerGhostManager;
import com.worldql.client.serialization.Codec;
import com.worldql.client.serialization.Instruction;
import com.worldql.client.serialization.Message;
import com.worldql.client.serialization.Vec3D;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import zmq.ZMQ;

import java.nio.ByteBuffer;

public class PlayerJoinEventListener implements Listener {
    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        //WorldQLClient.logger.info("Setting player " + e.getPlayer().getDisplayName() + " to get ghost join packets sent.");
        PlayerGhostManager.playerNeedsGhosts.put(e.getPlayer().getUniqueId(), true);
        WorldQLClient.getPluginInstance().getPacketReader().inject(e.getPlayer());
        Player player = e.getPlayer();

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
                new Vec3D(player.getLocation()),
                null,
                null,
                "MinecraftPlayerMove",
                bb
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
    }
}
