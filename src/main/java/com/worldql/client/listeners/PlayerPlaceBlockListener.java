package com.worldql.client.listeners;

import com.worldql.client.MinecraftUtil;
import com.worldql.client.WorldQLClient;
import com.worldql.client.serialization.Instruction;
import com.worldql.client.serialization.Message;
import com.worldql.client.serialization.Vec3D;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;

public class PlayerPlaceBlockListener {

    @EventHandler
    public void onPlayerPlaceBlockEvent(BlockPlaceEvent e) {
        Message message = new Message(
                Instruction.LocalMessage,
                WorldQLClient.worldQLClientId,
                e.getPlayer().getWorld().getName(),
                new Vec3D(MinecraftUtil.roundLocation(e.getBlock().getLocation())),
                null,
                null,
                "MinecraftBlockUpdate",
                null
        );
    }
}
