package com.worldql.client.listeners;

import com.worldql.client.MessageCodec;
import com.worldql.client.WorldQLClient;
import com.worldql.client.Messages.Instruction;
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

        for (int i = min_height; i <= max_height; i += 16) {
            MessageCodec.Vec3D position = new MessageCodec.Vec3D((float) x, (float) i, (float) z);
            byte[] buf = MessageCodec.encodeMessage(
                    WorldQLClient.worldQLClientId,
                    Instruction.AreaSubscribe,
                    chunk.getWorld().getName(),
                    position,
                    null,
                    null
            );

            WorldQLClient.getPluginInstance().getPushSocket().send(buf, ZMQ.ZMQ_DONTWAIT);
        }
    }
}
