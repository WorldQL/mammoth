package com.worldql.client.listeners.chunks;

import com.worldql.client.WorldQLClient;
import com.worldql.client.serialization.Instruction;
import com.worldql.client.serialization.Message;
import com.worldql.client.serialization.Vec3D;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import zmq.ZMQ;

public class ChunkLoadEventListener implements Listener {

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        Chunk chunk = e.getChunk();

        // Multiply coords by 16
        int x = chunk.getX() << 4;
        int z = chunk.getZ() << 4;

        int min_height = chunk.getWorld().getMinHeight();
        int max_height = chunk.getWorld().getMaxHeight();
        int middle = (min_height + max_height) / 2;

        Message recordMessage = new Message(
                Instruction.RecordRead,
                WorldQLClient.worldQLClientId,
                chunk.getWorld().getName(),
                new Vec3D(x, middle, z)
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(recordMessage.encode(), ZMQ.ZMQ_DONTWAIT);

        for (int i = min_height; i <= max_height; i += 16) {
            Vec3D position = new Vec3D(x, i, z);
            Message subMessage = new Message(
                    Instruction.AreaSubscribe,
                    WorldQLClient.worldQLClientId,
                    chunk.getWorld().getName(),
                    position
            );

            WorldQLClient.getPluginInstance().getPushSocket().send(subMessage.encode(), ZMQ.ZMQ_DONTWAIT);
        }
    }
}
