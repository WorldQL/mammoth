package com.worldql.client;

import WorldQLFB.StandardEvents.Update;
import WorldQLFB.StandardEvents.Vec3;
import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.compiled_protobuf.MinecraftPlayer;
import com.worldql.client.compiled_protobuf.WorldQLQuery;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class PlayerCrouchListener implements Listener {
    @EventHandler
    public void onCrouch(PlayerToggleSneakEvent e) {
        Location l = e.getPlayer().getLocation();


        FlatBufferBuilder builder = new FlatBufferBuilder(1024);
        int uuid = builder.createString(e.getPlayer().getUniqueId().toString());
        int name = builder.createString(e.getPlayer().getName());
        int instruction = builder.createString("MinecraftPlayerMove");
        int crouch_action = builder.createString(e.isSneaking() ? "crouch" : "uncrouch");
        int[] actions_array = {crouch_action};
        int actions = Update.createEntityactionsVector(builder, actions_array);
        Update.startUpdate(builder);
        Update.addUuid(builder, uuid);
        Update.addPosition(builder, Vec3.createVec3(builder, (float)l.getX(), (float)l.getY(), (float)l.getZ()));
        Update.addPitch(builder, l.getPitch());
        Update.addYaw(builder, l.getYaw());
        Update.addName(builder, name);
        Update.addInstruction(builder, instruction);
        Update.addEntityactions(builder, actions);
        int player = Update.endUpdate(builder);
        builder.finish(player);

        byte[] buf = builder.sizedByteArray();
        WorldQLClient.push_socket.send(buf, 0);

    }
}
