package com.worldql.client.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandUnstuck implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            int topY = player.getWorld().getHighestBlockYAt(player.getLocation());
            Location l = player.getLocation();
            l.setY(topY + 1);
            player.teleport(l);
        }
        return false;
    }
}
