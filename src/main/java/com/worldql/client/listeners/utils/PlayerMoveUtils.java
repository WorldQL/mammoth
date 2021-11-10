package com.worldql.client.listeners.utils;

import WorldQLFB.StandardEvents.Update;
import WorldQLFB.StandardEvents.Vec3;
import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.WorldQLClient;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import zmq.ZMQ;

import java.util.Arrays;
import java.util.Optional;

public class PlayerMoveUtils {

    public static void sendPacket(Location location, Player player, String[] actionsArray) {
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);

        int uuid = builder.createString(player.getUniqueId().toString());
        int name = builder.createString(player.getName());
        int instruction = builder.createString("MinecraftPlayerMove");

        Optional<Integer> actions = Optional.empty();

        if (actionsArray.length > 0) {
            int[] actionsIntArray = Arrays.stream(actionsArray).mapToInt(builder::createString).toArray();
            actions = Optional.of(Update.createEntityactionsVector(builder, actionsIntArray));
        }

        Update.startUpdate(builder);
        Update.addUuid(builder, uuid);
        Update.addPosition(builder, Vec3.createVec3(builder, (float) location.getX(), (float) location.getY(), (float) location.getZ()));
        Update.addPitch(builder, location.getPitch());
        Update.addYaw(builder, location.getYaw());
        Update.addName(builder, name);
        Update.addInstruction(builder, instruction);
        Update.addSenderid(builder, WorldQLClient.getPluginInstance().getZmqPortClientId());

        actions.ifPresent(integer -> Update.addEntityactions(builder, integer));

        int playerInt = Update.endUpdate(builder);
        builder.finish(playerInt);

        byte[] buf = builder.sizedByteArray();
        WorldQLClient.getPluginInstance().getPushSocket().send(buf, ZMQ.ZMQ_DONTWAIT);
    }

}
