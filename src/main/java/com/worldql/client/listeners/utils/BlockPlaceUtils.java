package com.worldql.client.listeners.utils;

import WorldQLFB.StandardEvents.Update;
import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.listeners.PlayerBlockListener;
import org.bukkit.Location;
import org.bukkit.block.Block;
import zmq.ZMQ;

import java.util.ArrayList;
import java.util.Optional;

public class BlockPlaceUtils {

    public static void sendPacket(Block block, Optional<SignEditData> signEditData) {
        Location location = block.getLocation();

        FlatBufferBuilder builder = new FlatBufferBuilder(1024);

        int instruction = builder.createString("MinecraftBlockPlace");
        int blockData = builder.createString(block.getBlockData().getAsString());
        int worldName = builder.createString(block.getWorld().getName());
        int[] paramsArray = {blockData};
        int params = Update.createParamsVector(builder, paramsArray);
        Optional<Integer> commands = Optional.empty();

        if (signEditData.isPresent()) {
            int command = builder.createString("update_sign");
            int signData = builder.createString(String.join("\n", signEditData.get().signContent));

            commands = Optional.of(Update.createCommandsVector(builder, new int[]{command, signData}));
        }

        Update.startUpdate(builder);
        Update.addInstruction(builder, instruction);
        Update.addWorldName(builder, worldName);
        Update.addPosition(builder, PlayerBlockListener.createRoundedVec3(builder, location.getX(), location.getY(), location.getZ()));
        Update.addParams(builder, params);
        Update.addSenderid(builder, WorldQLClient.getPluginInstance().getZmqPortClientId());

        commands.ifPresent(integer -> Update.addCommands(builder, integer));

        int blockUpdate = Update.endUpdate(builder);
        builder.finish(blockUpdate);

        byte[] buf = builder.sizedByteArray();
        WorldQLClient.getPluginInstance().getPushSocket().send(buf, ZMQ.ZMQ_DONTWAIT);
    }

    public static class SignEditData {

        ArrayList<String> signContent;

        public SignEditData(ArrayList<String> signContent) {
            this.signContent = signContent;
        }

    }

}
