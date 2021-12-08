package com.worldql.client.listeners.world;

import com.worldql.client.WorldQLClient;
import com.worldql.client.listeners.utils.BlockTools;
import com.worldql.client.serialization.Record;
import com.worldql.client.serialization.*;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import zmq.ZMQ;

import java.util.List;

public class PlayerEditSignListener implements Listener {
    @EventHandler
    public void onSignEdit(SignChangeEvent e) {
        if (e.getBlock().getState() instanceof Sign sign) {
            for (int i = 0; i < e.getLines().length; i++) {
                String line = e.getLines()[i];
                sign.setLine(i, line);
            }

            sign.update();
        }

        Record placedBlock = BlockTools.serializeBlock(e.getBlock());
        Message message = new Message(
                Instruction.RecordCreate,
                WorldQLClient.worldQLClientId,
                e.getPlayer().getWorld().getName(),
                Replication.ExceptSelf,
                // This field isn't really used since the Record also contains the position
                // of the changed block(s).
                new Vec3D(e.getBlock().getLocation()),
                List.of(placedBlock),
                null,
                "MinecraftBlockUpdate",
                null
        );
        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);

        // send a LocalMessage instruction with the same information so that clients can get an update on the chunk.
        Message localMessage = message.withInstruction(Instruction.LocalMessage);
        WorldQLClient.getPluginInstance().getPushSocket().send(localMessage.encode(), ZMQ.ZMQ_DONTWAIT);
    }
}
