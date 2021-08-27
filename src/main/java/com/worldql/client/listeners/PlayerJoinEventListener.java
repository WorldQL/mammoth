package com.worldql.client.listeners;

import com.worldql.client.PacketReader;
import com.worldql.client.WorldQLClient;
import com.worldql.client.ghost.PlayerGhostManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinEventListener implements Listener {
    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        //WorldQLClient.logger.info("Setting player " + e.getPlayer().getDisplayName() + " to get ghost join packets sent.");
        PlayerGhostManager.playerNeedsGhosts.put(e.getPlayer().getUniqueId(), true);
        PacketReader reader = new PacketReader();
        reader.inject(e.getPlayer());
    }
}
