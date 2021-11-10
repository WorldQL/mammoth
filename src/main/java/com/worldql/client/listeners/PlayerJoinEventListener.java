package com.worldql.client.listeners;

import com.worldql.client.WorldQLClient;
import com.worldql.client.ghost.PlayerGhostManager;
import com.worldql.client.listeners.utils.PlayerMoveUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinEventListener implements Listener {
    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        PlayerGhostManager.playerNeedsGhosts.put(e.getPlayer().getUniqueId(), true);
        WorldQLClient.getPluginInstance().getPacketReader().inject(e.getPlayer());

        // Sending move packet to show players on join
        PlayerMoveUtils.sendPacket(
                e.getPlayer().getLocation(),
                e.getPlayer(),
                new String[]{}
        );
    }
}
