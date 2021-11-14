package com.worldql.client.listeners;

import com.worldql.client.listeners.utils.BlockPlaceUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.util.Arrays;
import java.util.Optional;

public class PlayerEditSignListener implements Listener {
    @EventHandler
    public void onSignEdit(SignChangeEvent event) {
        BlockPlaceUtils.sendPacket(
                event.getBlock(),
                Optional.of(new BlockPlaceUtils.SignEditData(Arrays.asList(event.getLines())))
        );
    }
}
