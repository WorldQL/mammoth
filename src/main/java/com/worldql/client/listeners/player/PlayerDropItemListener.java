package com.worldql.client.listeners.player;

import com.worldql.client.Slices;
import com.worldql.client.WorldQLClient;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

public class PlayerDropItemListener implements Listener {
    @EventHandler
    public void onPlayerDropEvent(PlayerDropItemEvent e) {
        if (!WorldQLClient.getPluginInstance().playerDataSavingManager.isFullySynced(e.getPlayer())) {
            e.setCancelled(true);
            return;
        }
        if (Slices.getDistanceFromDMZ(e.getPlayer().getLocation()) < 5) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.GOLD + "You don't want to drop items on the ground here!" +
                    ChatColor.BOLD + " Move further from a server border.");
        }
    }
}
