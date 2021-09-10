package com.worldql.client.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;

public class BlockDropItemEventListener implements Listener {

    @EventHandler
    public void onBlockDrop(BlockDropItemEvent event) {
        // cancel the event because the item drop gets triggered through callback.
        event.setCancelled(true);
    }
}
