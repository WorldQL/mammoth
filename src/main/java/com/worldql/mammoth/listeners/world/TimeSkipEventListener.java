package com.worldql.mammoth.listeners.world;

import com.worldql.mammoth.MammothPlugin;
import com.worldql.mammoth.worldql_serialization.Instruction;
import com.worldql.mammoth.worldql_serialization.Message;
import com.worldql.mammoth.worldql_serialization.Replication;
import com.worldql.mammoth.worldql_serialization.Vec3D;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.TimeSkipEvent;
import zmq.ZMQ;

public class TimeSkipEventListener implements Listener {
    @EventHandler
    public void onTimeSkip(TimeSkipEvent e) {
        if (e.getSkipReason().equals(TimeSkipEvent.SkipReason.NIGHT_SKIP)) {
            Message message = new Message(
                    Instruction.GlobalMessage,
                    MammothPlugin.worldQLClientId,
                    "@global",
                    Replication.ExceptSelf,
                    new Vec3D(0,0,0),
                    null,
                    null,
                    "MinecraftNightSkip",
                    null
            );
            MammothPlugin.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
        }
    }
}
