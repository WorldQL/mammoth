package com.worldql.client.listeners;

import com.google.flatbuffers.FlatBufferBuilder;
import com.sun.jna.StringArray;
import com.worldql.client.WorldQLClient;
import com.worldql.client.listeners.utils.PlayerMoveUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import WorldQLFB.StandardEvents.*;
import zmq.ZMQ;

public class PlayerMoveAndLookHandler implements Listener {


    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent e) {
        if (e.getTo() == null) return;

        PlayerMoveUtils.sendPacket(
                e.getPlayer().getLocation(),
                e.getPlayer(),
                new String[]{}
        );
    }
}
