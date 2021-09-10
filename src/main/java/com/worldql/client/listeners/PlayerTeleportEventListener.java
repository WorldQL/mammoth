package com.worldql.client.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerTeleportEventListener implements Listener {

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event){
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL || event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            event.setCancelled(true);
            // temporarily disable nether and end portals
            // todo: remove this.
        }
    }
}
