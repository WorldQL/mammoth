package com.worldql.client.listeners;

import WorldQLFB.StandardEvents.Update;
import WorldQLFB.StandardEvents.Vec3;
import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.WorldQLClient;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import zmq.ZMQ;

public class PlayerLogOutListener implements Listener {

    @EventHandler
    public void onPlayerLogOut(PlayerQuitEvent e) {
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);
        int uuid = builder.createString(e.getPlayer().getUniqueId().toString());
        int instruction = builder.createString("MinecraftPlayerQuit");

        Update.startUpdate(builder);
        Update.addUuid(builder, uuid);
        Update.addInstruction(builder, instruction);
        Update.addSenderid(builder, WorldQLClient.getPluginInstance().getZmqPortClientId());

        int player = Update.endUpdate(builder);
        builder.finish(player);

        byte[] buf = builder.sizedByteArray();

        WorldQLClient.getPluginInstance().getPushSocket().send(buf, ZMQ.ZMQ_DONTWAIT);
    }
}


