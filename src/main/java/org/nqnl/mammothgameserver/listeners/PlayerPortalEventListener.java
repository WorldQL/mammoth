package org.nqnl.mammothgameserver.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.nqnl.mammothgameserver.MammothGameserver;
import org.nqnl.mammothgameserver.util.PlayerTransfer;

public class PlayerPortalEventListener implements Listener {
    private static MammothGameserver instance;
    public PlayerPortalEventListener(MammothGameserver _instance) {
        this.instance = _instance;
    }

    @EventHandler
    public void onPlayerPortalEvent(PlayerPortalEvent event) {
        PlayerTransfer.handleNetherPortal(event, this.instance);
    }

    @EventHandler
    public void onEntityPortalEvent(EntityPortalEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            event.setCancelled(true);
        }
    }
}
