package com.worldql.client.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

// TODO: Remove this entire class.
public class NotImplementedCanceller implements Listener {
    @EventHandler
    public void onRedstoneCurrent(BlockRedstoneEvent e) {
        e.setNewCurrent(0);
    }
}
