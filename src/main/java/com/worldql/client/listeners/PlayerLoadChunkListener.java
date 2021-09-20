package com.worldql.client.listeners;

import WorldQLFB.StandardEvents.Update;
import WorldQLFB.StandardEvents.Vec3;
import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.WorldQLClient;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import zmq.ZMQ;

public class PlayerLoadChunkListener implements Listener {

    @EventHandler
    public void onPlayerLoadChunk(ChunkLoadEvent e) {
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);
        int instruction = builder.createString("Mammoth.ChunkLookup");
        float[] numericalParamsArray = {
                0, 256
        };

        int numericalParams = Update.createNumericalParamsVector(builder, numericalParamsArray);

        Update.startUpdate(builder);
        Update.addPosition(builder, Vec3.createVec3(builder, e.getChunk().getX(), 0, e.getChunk().getZ()));
        Update.addInstruction(builder, instruction);
        Update.addSenderid(builder, WorldQLClient.getPluginInstance().getZmqPortClientId());
        Update.addNumericalParams(builder, numericalParams);

        int player = Update.endUpdate(builder);
        builder.finish(player);

        byte[] buf = builder.sizedByteArray();
        WorldQLClient.getPluginInstance().getPushSocket().send(buf, ZMQ.ZMQ_DONTWAIT);
    }
}
