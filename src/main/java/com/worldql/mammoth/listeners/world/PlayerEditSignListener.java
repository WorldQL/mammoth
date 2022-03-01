package com.worldql.mammoth.listeners.world;

import com.worldql.mammoth.Slices;
import com.worldql.mammoth.MammothPlugin;
import com.worldql.mammoth.listeners.utils.BlockTools;
import com.worldql.mammoth.worldql_serialization.Record;
import com.worldql.mammoth.worldql_serialization.*;
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
                MammothPlugin.worldQLClientId,
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

        if (!Slices.enabled) {
            MammothPlugin.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);

            // send a LocalMessage instruction with the same information so that clients can get an update on the chunk.
            Message localMessage = message.withInstruction(Instruction.LocalMessage);
            MammothPlugin.getPluginInstance().getPushSocket().send(localMessage.encode(), ZMQ.ZMQ_DONTWAIT);
        } else if (Slices.enabled && Slices.isDMZ(e.getBlock().getLocation())) {
            Message globalMessage = message.withInstruction(Instruction.LocalMessage);
            MammothPlugin.getPluginInstance().getPushSocket().send(globalMessage.encode(), ZMQ.ZMQ_DONTWAIT);
        }
    }
}
