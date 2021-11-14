package com.worldql.client;

import WorldQLFB.StandardEvents.Update;
import com.google.flatbuffers.FlatBufferBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import zmq.ZMQ;

public class TestRefreshWorldCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player player) {
            FlatBufferBuilder bufferBuilder = new FlatBufferBuilder(256);
            int instruction = bufferBuilder.createString("Record.Get.Blocks.all");
            int world = bufferBuilder.createString(player.getWorld().getName());

            Update.startUpdate(bufferBuilder);
            Update.addSenderid(bufferBuilder, Bukkit.getServer().getPort());
            Update.addWorldName(bufferBuilder, world);
            Update.addInstruction(bufferBuilder, instruction);
            Update.addSenderid(bufferBuilder, WorldQLClient.getPluginInstance().getZmqPortClientId());

            int update = Update.endUpdate(bufferBuilder);
            bufferBuilder.finish(update);

            byte[] buf = bufferBuilder.sizedByteArray();
            WorldQLClient.getPluginInstance().getPushSocket().send(buf, ZMQ.ZMQ_DONTWAIT);
        }

        return false;
    }
}
