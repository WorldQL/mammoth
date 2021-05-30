package com.worldql.client;

import WorldQLFB.StandardEvents.Update;
import com.google.flatbuffers.FlatBufferBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TestRefreshWorldCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            FlatBufferBuilder b = new FlatBufferBuilder(512);
            int instruction = b.createString("Record.Get.Blocks.all");
            int world = b.createString(((Player) sender).getWorld().getName());
            Update.startUpdate(b);
            Update.addSenderid(b, Bukkit.getServer().getPort());
            Update.addWorldName(b, world);
            Update.addInstruction(b, instruction);
            int update = Update.endUpdate(b);
            b.finish(update);

            byte[] buf = b.sizedByteArray();
            WorldQLClient.push_socket.send(buf, 0);
        }


        return false;
    }
}
