package org.nqnl.mammothgameserver.listeners;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.nqnl.mammothgameserver.SliceMethods;

public class EntityExplodeEventLister implements Listener {
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        for (Block block : e.blockList()) {
            if (SliceMethods.getDMZStatus(block.getLocation().getX())) {
                e.setCancelled(true);
                return;
            }
        }
    }
}
