package com.worldql.client.listeners.world;

import com.worldql.client.WorldQLClient;
import com.worldql.client.worldql_serialization.Instruction;
import com.worldql.client.worldql_serialization.Message;
import com.worldql.client.worldql_serialization.Replication;
import com.worldql.client.worldql_serialization.Vec3D;
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
                    WorldQLClient.worldQLClientId,
                    "@global",
                    Replication.ExceptSelf,
                    new Vec3D(0,0,0),
                    null,
                    null,
                    "MinecraftNightSkip",
                    null
            );
            WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
        }
    }
}
