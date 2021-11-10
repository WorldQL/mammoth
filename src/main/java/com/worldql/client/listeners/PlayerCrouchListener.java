package com.worldql.client.listeners;

import WorldQLFB.StandardEvents.Update;
import WorldQLFB.StandardEvents.Vec3;
import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.listeners.utils.PlayerMoveUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import zmq.ZMQ;

public class PlayerCrouchListener implements Listener {
    @EventHandler
    public void onCrouch(PlayerToggleSneakEvent e) {
        PlayerMoveUtils.sendPacket(
                e.getPlayer().getLocation(),
                e.getPlayer(),
                new String[]{e.isSneaking() ? "crouch" : "uncrouch"}
        );
    }
}
