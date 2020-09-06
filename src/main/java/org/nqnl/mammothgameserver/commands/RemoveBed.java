package org.nqnl.mammothgameserver.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nqnl.mammothgameserver.MammothGameserver;
import redis.clients.jedis.Jedis;

public class RemoveBed implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Jedis j = MammothGameserver.pool.getResource();
            Player player = ((Player) sender);
            try {
                j.del("bed-" + player.getUniqueId().toString());
                player.sendMessage("You Have Removed Your Bed!");
            } catch (Exception e) {
                e.printStackTrace();
                player.sendMessage("There Was An Error Removing Your Bed, Please try again!");
            } finally {
                j.close();
            }
        }
        return true;
    }
}
