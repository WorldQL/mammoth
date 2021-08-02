package com.worldql.client.listeners;

import WorldQLFB.StandardEvents.Update;
import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.events.OutgoingPlayerHitEvent;
import com.worldql.client.ghost.ExpiringEntityPlayer;
import com.worldql.client.ghost.PlayerGhostManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class OutgoingPlayerHitListener implements Listener {

    @EventHandler
    public void onPlayerHit(OutgoingPlayerHitEvent e) {
        System.out.println(e.getPlayerId());
        System.out.println("SENDING OUTGOING PLAYER HIT EVENT");

        ExpiringEntityPlayer hitPlayer = PlayerGhostManager.integerNPCLookup.get(e.getPlayerId());
        String UUIDString = hitPlayer.grab().getUniqueIDString();
        System.out.println(UUIDString);

        FlatBufferBuilder builder = new FlatBufferBuilder(1024);
        int instruction = builder.createString("EntityHitEvent");
        int playerUUID = builder.createString(UUIDString);
        int[] params_array = {playerUUID};
        int params = Update.createParamsVector(builder, params_array);
        Update.startUpdate(builder);
        Update.addInstruction(builder, instruction);
        Update.addParams(builder, params);
        Update.addSenderid(builder, WorldQLClient.zmqPortClientId);
        int update = Update.endUpdate(builder);
        builder.finish(update);

        byte[] buf = builder.sizedByteArray();
        WorldQLClient.push_socket.send(buf, 0);

    }
}
