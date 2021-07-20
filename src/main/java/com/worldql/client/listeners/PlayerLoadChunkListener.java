package com.worldql.client.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class PlayerLoadChunkListener implements Listener {

    @EventHandler
    public void onPlayerLoadChunk(ChunkLoadEvent e) {
        if (e.isNewChunk()) {
        }
    }
}
