package com.worldql.client.listeners.player;

import com.worldql.client.WorldQLClient;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

public class PlayerDropItemListener implements Listener {
    @EventHandler
    public void onPlayerDropEvent(PlayerDropItemEvent e) {
        if (!WorldQLClient.getPluginInstance().playerDataSavingManager.isSafe(e.getPlayer())) {
            e.setCancelled(true);
        }
    }
}
