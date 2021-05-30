package com.worldql.client.listeners;

import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.WorldQLClient;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import WorldQLFB.StandardEvents.*;

public class PlayerMoveAndLookHandler implements Listener {


    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent e) {
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);
        int uuid = builder.createString(e.getPlayer().getUniqueId().toString());
        int name = builder.createString(e.getPlayer().getName());
        int instruction = builder.createString("MinecraftPlayerMove");
        Update.startUpdate(builder);
        Update.addUuid(builder, uuid);
        Update.addPosition(builder, Vec3.createVec3(builder, (float)e.getTo().getX(), (float)e.getTo().getY(), (float)e.getTo().getZ()));
        Update.addPitch(builder, e.getTo().getPitch());
        Update.addYaw(builder, e.getTo().getYaw());
        Update.addName(builder, name);
        Update.addInstruction(builder, instruction);
        int player = Update.endUpdate(builder);
        builder.finish(player);
        byte[] buf = builder.sizedByteArray();
        WorldQLClient.push_socket.send(buf, 0);
    }
}
