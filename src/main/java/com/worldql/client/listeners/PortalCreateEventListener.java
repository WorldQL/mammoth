package com.worldql.client.listeners;

import WorldQLFB.StandardEvents.Update;
import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.WorldQLClient;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;

public class PortalCreateEventListener implements Listener {
    @EventHandler
    public void onPortalCreate(PortalCreateEvent e) {
        for (BlockState b : e.getBlocks()) {
            if (b.getBlockData().getMaterial().equals(Material.NETHER_PORTAL)) {
                Location l = b.getLocation();
                FlatBufferBuilder builder = new FlatBufferBuilder(1024);
                int instruction = builder.createString("MinecraftBlockPlace");
                int blockdata = builder.createString(b.getBlockData().getAsString());
                int worldName = builder.createString(b.getBlock().getWorld().getName());
                int[] params_array = {blockdata};
                int params = Update.createParamsVector(builder, params_array);
                Update.startUpdate(builder);
                Update.addInstruction(builder, instruction);
                Update.addWorldName(builder, worldName);
                Update.addPosition(builder, PlayerBlockPlaceListener.createRoundedVec3(builder, l.getX(), l.getY(), l.getZ()));
                Update.addParams(builder, params);
                int blockupdate = Update.endUpdate(builder);
                builder.finish(blockupdate);

                byte[] buf = builder.sizedByteArray();
                WorldQLClient.push_socket.send(buf, 0);
            }
        }
    }
}
