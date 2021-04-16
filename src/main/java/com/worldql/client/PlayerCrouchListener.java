package com.worldql.client;

import com.worldql.client.compiled_protobuf.MinecraftPlayer;
import com.worldql.client.compiled_protobuf.WorldQLQuery;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class PlayerCrouchListener implements Listener {
    @EventHandler
    public void onCrouch(PlayerToggleSneakEvent e) {
        Location l = e.getPlayer().getLocation();
        MinecraftPlayer.PlayerState playerState = MinecraftPlayer.PlayerState.newBuilder()
                .setX((float) l.getX())
                .setY((float) l.getY())
                .setZ((float) l.getZ())
                .setPitch(l.getPitch())
                .setYaw(l.getYaw())
                .setUUID(e.getPlayer().getUniqueId().toString())
                .setName(e.getPlayer().getName())
                .setAction(e.isSneaking() ? "crouch" : "uncrouch")
                .build();
        System.out.println("SENDING CROUCH");
        System.out.println(e.isSneaking());
        WorldQLQuery.WQL message = WorldQLQuery.WQL.newBuilder().setPlayerState(playerState).build();
        WorldQLClient.push_socket.send(message.toByteArray(), 0);

    }
}
