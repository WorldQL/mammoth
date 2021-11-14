package com.worldql.client.listeners;

import WorldQLFB.StandardEvents.Update;
import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.events.OutgoingPlayerHitEvent;
import com.worldql.client.ghost.ExpiringEntityPlayer;
import com.worldql.client.ghost.PlayerGhostManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import zmq.ZMQ;

public class OutgoingPlayerHitListener implements Listener {

    @EventHandler
    public void onPlayerHit(OutgoingPlayerHitEvent event) {
        ExpiringEntityPlayer hitPlayer = PlayerGhostManager.integerNPCLookup.get(event.getPlayerId());

        if (hitPlayer == null) {
            return;
        }

        String uuidString = hitPlayer.grab().getUniqueIDString();

        FlatBufferBuilder builder = new FlatBufferBuilder(1024);

        int instruction = builder.createString("EntityHitEvent");
        int playerUUID = builder.createString(uuidString);

        int[] paramsArray = {playerUUID};
        float[] numericalParamsArray = {
                (float) event.getDirection().getX(),
                (float) event.getDirection().getY(),
                (float) event.getDirection().getZ()
        };

        int params = Update.createParamsVector(builder, paramsArray);
        int numericalParams = Update.createNumericalParamsVector(builder, numericalParamsArray);

        Update.startUpdate(builder);
        Update.addInstruction(builder, instruction);
        Update.addParams(builder, params);
        Update.addNumericalParams(builder, numericalParams);
        Update.addSenderid(builder, WorldQLClient.getPluginInstance().getZmqPortClientId());

        int update = Update.endUpdate(builder);
        builder.finish(update);

        byte[] buf = builder.sizedByteArray();
        WorldQLClient.getPluginInstance().getPushSocket().send(buf, ZMQ.ZMQ_DONTWAIT);
    }
}
