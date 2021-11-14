package com.worldql.client.listeners;

import com.worldql.client.listeners.utils.PlayerMoveUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractEventListener implements Listener {
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        PlayerMoveUtils.sendPacket(
                event.getPlayer().getLocation(),
                event.getPlayer(),
                new String[]{"punch"}
        );
    }
}
