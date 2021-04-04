package com.worldql.client;

import com.worldql.client.compiled_protobuf.MinecraftPlayer;
import com.worldql.client.compiled_protobuf.WorldQLQuery;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class PlayerMoveAndLookHandler implements Listener {
    private ZMQ.Socket socket;

    public PlayerMoveAndLookHandler(ZMQ.Socket socket) {
        this.socket = socket;
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent e) {
        System.out.println(e.getTo().getPitch() + " " + e.getTo().getYaw());
        System.out.println(e.getTo().getX());
        String hi = Float.toString(e.getTo().getPitch());

        MinecraftPlayer.PlayerState playerState = MinecraftPlayer.PlayerState.newBuilder().setX((float) e.getTo().getX())
                .setY((float) e.getTo().getY())
                .setZ((float) e.getTo().getZ())
                .setPitch(e.getTo().getPitch())
                .setYaw(e.getTo().getYaw())
                .setUUID(e.getPlayer().getUniqueId().toString())
                .setName(e.getPlayer().getName())
                .build();
        WorldQLQuery.WQL message = WorldQLQuery.WQL.newBuilder().setPlayerState(playerState).build();

        socket.send(message.toByteArray(), 0);

    }
}
