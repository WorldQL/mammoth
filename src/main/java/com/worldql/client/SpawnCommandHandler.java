package com.worldql.client;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.worldql.client.ghost.PacketNPC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.comphenix.protocol.ProtocolManager;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;

public class SpawnCommandHandler implements CommandExecutor {
    private ProtocolManager manager;
    private Random random;
    public SpawnCommandHandler(ProtocolManager manager) {
        this.manager = manager;
        this.random = new Random();
    }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        sender.sendMessage("Hey there champ!");
        Player player = (Player)sender;
        PacketContainer fakeExplosion = manager.
                createPacket(PacketType.Play.Server.EXPLOSION);


        fakeExplosion.getDoubles().
                write(0, player.getLocation().getX()).
                write(1, player.getLocation().getY()).
                write(2, player.getLocation().getZ());
        fakeExplosion.getFloat().write(0, 3.0F);

        try {
            manager.sendServerPacket(player, fakeExplosion);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(
                    "Cannot send packet " + fakeExplosion, e);
        }



        return false;
    }
}
