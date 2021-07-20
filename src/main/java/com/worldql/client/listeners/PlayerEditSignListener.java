package com.worldql.client.listeners;

import WorldQLFB.StandardEvents.Update;
import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.WorldQLClient;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

public class PlayerEditSignListener implements Listener {
    @EventHandler
    public void onSignEdit(SignChangeEvent e) {
        Location l = e.getBlock().getLocation();
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);
        int instruction = builder.createString("MinecraftBlockPlace");
        int blockdata = builder.createString(e.getBlock().getBlockData().getAsString());
        int command = builder.createString("update_sign");
        int signdata = builder.createString(String.join("\n", e.getLines()));
        int[] commands_array = {command, signdata};
        int commands = Update.createCommandsVector(builder, commands_array);
        int[] params_array = {blockdata};
        int params = Update.createParamsVector(builder, params_array);
        int worldName = builder.createString(e.getBlock().getWorld().getName());
        Update.startUpdate(builder);
        Update.addInstruction(builder, instruction);
        Update.addPosition(builder, PlayerBlockPlaceListener.createRoundedVec3(builder, l.getX(), l.getY(), l.getZ()));
        Update.addParams(builder, params);
        Update.addCommands(builder, commands);
        Update.addSenderid(builder, WorldQLClient.zmqPortClientId);
        Update.addWorldName(builder, worldName);
        int blockupdate = Update.endUpdate(builder);
        builder.finish(blockupdate);

        byte[] buf = builder.sizedByteArray();
        WorldQLClient.push_socket.send(buf, 0);
    }
}
