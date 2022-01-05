package com.worldql.client.listeners;

import com.worldql.client.Slices;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.world.StructureGrowEvent;

public class NotImplementedCanceller implements Listener {
    @EventHandler
    public void onRedstoneCurrent(BlockRedstoneEvent e) {
        if (Slices.enabled && Slices.isDMZ(e.getBlock().getLocation())) {
            e.setNewCurrent(0);
        }
        if (!Slices.enabled) {
            e.setNewCurrent(0);
        }
    }

    @EventHandler
    public void onLiquid(BlockFromToEvent e){
        if (Slices.enabled && Slices.isDMZ(e.getToBlock().getLocation())) {
            e.setCancelled(true);
        }
        if (!Slices.enabled) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBurn(BlockSpreadEvent e) {
        if (Slices.enabled && Slices.isDMZ(e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
        if (!Slices.enabled) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onStateChange(EntityChangeBlockEvent e) {
        if (Slices.enabled && Slices.isDMZ(e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
        if (!Slices.enabled) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBonemeal(StructureGrowEvent e) {
        if (Slices.enabled && Slices.isDMZ(e.getLocation())) {
            e.setCancelled(true);
        }
        if (!Slices.enabled) {
            e.setCancelled(true);
        }
    }
}
