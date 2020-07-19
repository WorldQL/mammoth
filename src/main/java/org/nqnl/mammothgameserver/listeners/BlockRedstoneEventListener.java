package org.nqnl.mammothgameserver.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.nqnl.mammothgameserver.SliceMethods;

public class BlockRedstoneEventListener implements Listener {
    @EventHandler
    public void onBlockRedstoneEvent(BlockRedstoneEvent event) {
        if (SliceMethods.getDMZStatus(event.getBlock().getLocation().getX())) {
            event.setNewCurrent(0);
        }
    }
}
