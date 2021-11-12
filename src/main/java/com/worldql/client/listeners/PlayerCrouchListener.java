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
import org.bukkit.event.player.PlayerToggleSneakEvent;
import zmq.ZMQ;

import java.nio.ByteBuffer;

public class PlayerCrouchListener implements Listener {
    @EventHandler
    public void onCrouch(PlayerToggleSneakEvent e) {
        Location playerLocation = e.getPlayer().getLocation();
        String action = e.isSneaking() ? "crouch" : "uncrouch";
        OutgoingMinecraftPlayerSingleAction.sendPacket(playerLocation, e.getPlayer(), action);
    }
}
