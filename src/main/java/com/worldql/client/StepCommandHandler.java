package com.worldql.client;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;

public class StepCommandHandler implements CommandExecutor {
    private ProtocolManager manager;
    private JavaPlugin plugin;
    public StepCommandHandler(ProtocolManager manager, JavaPlugin plugin) {
        this.manager = manager;
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        sender.sendMessage("Hey there champoo!");
        Player player = (Player)sender;

        BukkitTask task = new GhostWalkTask(plugin, 25, player).runTaskTimer(plugin, 10, 3);

        return false;
    }
}
