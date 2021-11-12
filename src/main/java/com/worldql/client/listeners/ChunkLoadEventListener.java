package com.worldql.client.listeners;

import WorldQLFB_OLD.StandardEvents.Update;
import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.Messages.Instruction;
import com.worldql.client.Messages.Message;
import com.worldql.client.Messages.Vec3d;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import zmq.ZMQ;

public class ChunkLoadEventListener implements Listener {

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        int x = e.getChunk().getX() * 16;
        int z = e.getChunk().getZ() * 16;

        int min_height = e.getChunk().getWorld().getMinHeight();
        int max_height = e.getChunk().getWorld().getMaxHeight();

        for (int i = min_height; i <= max_height; i += 16) {
            FlatBufferBuilder builder = new FlatBufferBuilder(1024);

            int sender_uuid = builder.createString(WorldQLClient.worldQLClientId);
            int worldName = builder.createString(e.getChunk().getWorld().getName());
            //int command = builder.createString("MinecraftPlayerMove");
            //int flex = builder.createByteVector(bb);

            Message.startMessage(builder);
            // Inform the other servers about this player movement with a LocalMessage
            Message.addInstruction(builder, Instruction.AreaSubscribe);
            Message.addWorldName(builder, worldName);
            // Store the "MinecraftPlayerMove" command in the parameter field
            // Message.addParameter(builder, command);
            Message.addSenderUuid(builder, sender_uuid);
            Message.addPosition(builder, Vec3d.createVec3d(builder, (float) x, (float) i, (float) z));
            //Message.addFlex(builder, flex);

            int message = Message.endMessage(builder);
            builder.finish(message);

            byte[] buf = builder.sizedByteArray();
            WorldQLClient.getPluginInstance().getPushSocket().send(buf, ZMQ.ZMQ_DONTWAIT);
        }
    }
}
