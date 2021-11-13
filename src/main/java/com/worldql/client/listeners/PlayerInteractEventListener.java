package com.worldql.client.listeners;

import WorldQLFB_OLD.StandardEvents.Update;
import WorldQLFB_OLD.StandardEvents.Vec3;
import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.Messages.Instruction;
import com.worldql.client.Messages.Message;
import com.worldql.client.Messages.Vec3d;
import com.worldql.client.WorldQLClient;
import com.worldql.client.listeners.utils.OutgoingMinecraftPlayerSingleAction;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import zmq.ZMQ;

import java.nio.ByteBuffer;

public class PlayerInteractEventListener implements Listener {
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
            return;
        Location playerLocation = e.getPlayer().getLocation();
        String action = "punch";
        OutgoingMinecraftPlayerSingleAction.sendPacket(playerLocation, e.getPlayer(), action);
    }
}
