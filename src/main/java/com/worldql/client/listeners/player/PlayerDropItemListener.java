package com.worldql.client.listeners.player;

import com.worldql.client.Slices;
import com.worldql.client.WorldQLClient;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

public class PlayerDropItemListener implements Listener {
    @EventHandler
    public void onPlayerDropEvent(PlayerDropItemEvent e) {
        if (!WorldQLClient.playerDataSavingManager.isFullySynced(e.getPlayer()) || WorldQLClient.playerDataSavingManager.getMsSinceLogin(e.getPlayer()) < 8000) {
            e.setCancelled(true);
            e.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(ChatColor.RED + "You can't move items right now. Please wait a moment..."));
            return;
        }
        if (Slices.getDistanceFromSliceBoundary(e.getPlayer().getLocation()) < 5) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.GOLD + "You don't want to drop items on the ground here!" +
                    ChatColor.BOLD + " Move further from a server border.");
        }
    }
}
