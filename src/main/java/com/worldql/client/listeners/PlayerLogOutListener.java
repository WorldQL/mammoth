package com.worldql.client.listeners;

import WorldQLFB.StandardEvents.Update;
import WorldQLFB.StandardEvents.Vec3;
import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.WorldQLClient;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerLogOutListener implements Listener {
    public void onPlayerLogOut(PlayerQuitEvent e) {
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);
        int uuid = builder.createString(e.getPlayer().getUniqueId().toString());
        int name = builder.createString(e.getPlayer().getName());
        int instruction = builder.createString("MinecraftPlayerQuit");
        Update.startUpdate(builder);
        Update.addUuid(builder, uuid);
        Update.addInstruction(builder, instruction);
        int player = Update.endUpdate(builder);
        builder.finish(player);

        byte[] buf = builder.sizedByteArray();


        WorldQLClient.push_socket.send(buf, 0);
        System.out.println("Sent successfully");
    }
}


