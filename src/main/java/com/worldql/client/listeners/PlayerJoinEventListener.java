package com.worldql.client.listeners;

import com.worldql.client.WorldQLClient;
import com.worldql.client.ghost.PlayerGhostManager;
import com.worldql.client.listeners.utils.PlayerMoveUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinEventListener implements Listener {
    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        PlayerGhostManager.playerNeedsGhosts.put(event.getPlayer().getUniqueId(), true);
        WorldQLClient.getPluginInstance().getPacketReader().inject(event.getPlayer());

        // Sending move packet to show players on join
        PlayerMoveUtils.sendPacket(
                event.getPlayer().getLocation(),
                event.getPlayer(),
                new String[]{}
        );
    }
}
