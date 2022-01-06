package com.worldql.client.listeners.explosions;

import com.worldql.client.Slices;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;

public class BlockExplodeEventListener implements Listener {
    @EventHandler
    public void onBlockExplodeEvent(BlockExplodeEvent e) {
        if (Slices.enabled && Slices.isDMZ(e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
    }
}
