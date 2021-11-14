package com.worldql.client.listeners;

import com.worldql.client.listeners.utils.PlayerMoveUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveAndLookHandler implements Listener {


    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        if (event.getTo() == null) return;

        PlayerMoveUtils.sendPacket(
                event.getPlayer().getLocation(),
                event.getPlayer(),
                new String[]{}
        );
    }
}
