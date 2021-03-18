package com.worldql.client;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveAndLookHandler implements Listener {

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent e) {
        System.out.println(e.getTo().getPitch() + " " + e.getTo().getYaw());
    }
}
