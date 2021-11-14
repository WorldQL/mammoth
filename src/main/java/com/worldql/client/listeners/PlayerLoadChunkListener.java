package com.worldql.client.listeners;

import WorldQLFB.StandardEvents.Update;
import WorldQLFB.StandardEvents.Vec3;
import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.WorldQLClient;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import zmq.ZMQ;

import java.util.Hashtable;

public class PlayerLoadChunkListener implements Listener {
    public static final Hashtable<String, Long> chunkLastRefreshed = new Hashtable<>();


    @EventHandler
    public void onPlayerLoadChunk(ChunkLoadEvent event) {
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);
        int instruction = builder.createString("Mammoth.ChunkLookup");
        float[] numericalParamsArray = {
                0, 256
        };

        String chunkKey = event.getChunk().getX() + "," + event.getChunk().getZ();

        if (chunkLastRefreshed.containsKey(chunkKey)) {
            if (System.currentTimeMillis() - chunkLastRefreshed.get(chunkKey) < 30000) {
                return;
            }
        }

        int numericalParams = Update.createNumericalParamsVector(builder, numericalParamsArray);

        Update.startUpdate(builder);
        Update.addPosition(builder, Vec3.createVec3(builder, event.getChunk().getX(), 0, event.getChunk().getZ()));
        Update.addInstruction(builder, instruction);
        Update.addSenderid(builder, WorldQLClient.getPluginInstance().getZmqPortClientId());
        Update.addNumericalParams(builder, numericalParams);

        int player = Update.endUpdate(builder);
        builder.finish(player);

        byte[] buf = builder.sizedByteArray();
        WorldQLClient.getPluginInstance().getPushSocket().send(buf, ZMQ.ZMQ_DONTWAIT);

        chunkLastRefreshed.put(chunkKey, System.currentTimeMillis());
    }
}
