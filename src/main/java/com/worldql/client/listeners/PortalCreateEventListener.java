package com.worldql.client.listeners;

import com.worldql.client.WorldQLClient;
import com.worldql.client.listeners.utils.BlockPlaceUtils;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;

import java.util.Optional;

public class PortalCreateEventListener implements Listener {

    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        WorldQLClient.getPluginInstance().getLogger().info(String.valueOf(event.getReason()));

        for (final BlockState b : event.getBlocks()) {
            if (b.getBlockData().getMaterial().equals(Material.NETHER_PORTAL) || b.getBlockData().getMaterial().equals(Material.OBSIDIAN)) {
                BlockPlaceUtils.sendPacket(b.getBlock(), Optional.empty());
            }
        }
    }

}
