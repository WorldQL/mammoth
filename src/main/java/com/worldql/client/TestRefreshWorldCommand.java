package com.worldql.client;

import WorldQLFB_OLD.StandardEvents.Update;
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
            FlatBufferBuilder b = new FlatBufferBuilder(256);
            int instruction = b.createString("Record.Get.Blocks.all");
            int world = b.createString(player.getWorld().getName());

            Update.startUpdate(b);
            Update.addSenderid(b, Bukkit.getServer().getPort());
            Update.addWorldName(b, world);
            Update.addInstruction(b, instruction);
            Update.addSenderid(b, WorldQLClient.getPluginInstance().getZmqPortClientId());

            int update = Update.endUpdate(b);
            b.finish(update);

            byte[] buf = b.sizedByteArray();
            WorldQLClient.getPluginInstance().getPushSocket().send(buf, ZMQ.ZMQ_DONTWAIT);
        }
        return false;
    }
}
