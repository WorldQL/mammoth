package com.worldql.client.listeners;

import WorldQLFB.StandardEvents.Update;
import WorldQLFB.StandardEvents.Vec3;
import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.WorldQLClient;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import zmq.ZMQ;

public class PlayerCrouchListener implements Listener {
    @EventHandler
    public void onCrouch(PlayerToggleSneakEvent e) {
        Location l = e.getPlayer().getLocation();

        FlatBufferBuilder builder = new FlatBufferBuilder(1024);

        int uuid = builder.createString(e.getPlayer().getUniqueId().toString());
        int name = builder.createString(e.getPlayer().getName());
        int instruction = builder.createString("MinecraftPlayerMove");
        int crouchAction = builder.createString(e.isSneaking() ? "crouch" : "uncrouch");
        int[] actionsArray = {crouchAction};
        int actions = Update.createEntityactionsVector(builder, actionsArray);

        Update.startUpdate(builder);
        Update.addUuid(builder, uuid);
        Update.addPosition(builder, Vec3.createVec3(builder, (float) l.getX(), (float) l.getY(), (float) l.getZ()));
        Update.addPitch(builder, l.getPitch());
        Update.addYaw(builder, l.getYaw());
        Update.addName(builder, name);
        Update.addInstruction(builder, instruction);
        Update.addEntityactions(builder, actions);
        Update.addSenderid(builder, WorldQLClient.getPluginInstance().getZmqPortClientId());

        int player = Update.endUpdate(builder);
        builder.finish(player);

        byte[] buf = builder.sizedByteArray();
        WorldQLClient.getPluginInstance().getPushSocket().send(buf, ZMQ.ZMQ_DONTWAIT);

    }
}
