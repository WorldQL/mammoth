package com.worldql.client.listeners;

import com.worldql.client.ghost.PlayerGhostManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinEventListener implements Listener {
    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        System.out.println("Setting player " + e.getPlayer().getDisplayName() + " to get ghost join packets sent.");
        PlayerGhostManager.playerNeedsGhosts.put(e.getPlayer().getUniqueId(), true);
    }
}
