package com.worldql.mammoth.listeners.chunks;

import com.worldql.mammoth.Slices;
import com.worldql.mammoth.MammothPlugin;
import com.worldql.mammoth.worldql_serialization.Instruction;
import com.worldql.mammoth.worldql_serialization.Message;
import com.worldql.mammoth.worldql_serialization.Replication;
import com.worldql.mammoth.worldql_serialization.Vec3D;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import zmq.ZMQ;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ChunkLoadEventListener implements Listener {
    private static final Map<Chunk, Long> seenChunks = Collections.synchronizedMap(new HashMap<>());

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        Chunk chunk = e.getChunk();

        // Multiply coords by 16
        int x = chunk.getX() << 4;
        int z = chunk.getZ() << 4;

        int min_height = chunk.getWorld().getMinHeight();
        int max_height = chunk.getWorld().getMaxHeight();

        if (!Slices.enabled) {
            String parameter = null;
            if (seenChunks.containsKey(chunk)) {
                long ts = seenChunks.get(chunk);
                parameter = Long.toString(ts);
            }

            Message recordMessage = new Message(
                    Instruction.RecordRead,
                    MammothPlugin.worldQLClientId,
                    chunk.getWorld().getName(),
                    Replication.ExceptSelf,
                    new Vec3D(x, 0, z),
                    null,
                    null,
                    parameter,
                    null
            );

            MammothPlugin.getPluginInstance().getPushSocket().send(recordMessage.encode(), ZMQ.ZMQ_DONTWAIT);

            // Handle Y=-1 to Y=-256
            if (min_height < 0) {
                Vec3D belowZero = new Vec3D(x, -1, z);
                Message belowZeroMessage = recordMessage.withPosition(belowZero);
                MammothPlugin.getPluginInstance().getPushSocket().send(belowZeroMessage.encode(), ZMQ.ZMQ_DONTWAIT);
            }

            // Handle Y=256 to Y=511
            if (max_height > 256) {
                Vec3D aboveWorld = new Vec3D(x, 256, z);
                Message aboveWorldMessage = recordMessage.withPosition(aboveWorld);
                MammothPlugin.getPluginInstance().getPushSocket().send(aboveWorldMessage.encode(), ZMQ.ZMQ_DONTWAIT);
            }

            seenChunks.put(chunk, System.currentTimeMillis());
        }

        if (MammothPlugin.processGhosts) {
            for (int i = min_height; i <= max_height; i += 16) {
                Vec3D position = new Vec3D(x, i, z);
                Message subMessage = new Message(
                        Instruction.AreaSubscribe,
                        MammothPlugin.worldQLClientId,
                        chunk.getWorld().getName(),
                        position
                );

                MammothPlugin.getPluginInstance().getPushSocket().send(subMessage.encode(), ZMQ.ZMQ_DONTWAIT);
            }
        }
    }
}
