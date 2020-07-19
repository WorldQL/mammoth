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
            try {
                j.del("bed-"+((Player) sender).getUniqueId().toString());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                j.close();
            }
        }
        return true;
    }
}
