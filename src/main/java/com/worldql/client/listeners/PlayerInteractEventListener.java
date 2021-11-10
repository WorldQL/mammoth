package com.worldql.client.listeners;

import WorldQLFB.StandardEvents.Update;
import WorldQLFB.StandardEvents.Vec3;
import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.listeners.utils.PlayerMoveUtils;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import zmq.ZMQ;

public class PlayerInteractEventListener implements Listener {
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        PlayerMoveUtils.sendPacket(
                e.getPlayer().getLocation(),
                e.getPlayer(),
                new String[]{"punch"}
        );
    }
}
