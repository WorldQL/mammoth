package com.worldql.client;

import com.worldql.client.ghost.PacketNPC;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class GhostWalkTask extends BukkitRunnable {

    private final JavaPlugin plugin;
    private int counter;
    private Player player;

    public GhostWalkTask(JavaPlugin plugin, int counter, Player player) {
        this.plugin = plugin;
        this.player = player;
        if (counter <= 0) {
            throw new IllegalArgumentException("Counter must be greater than zero!");
        } else {
            this.counter = counter;
        }
    }

    @Override
    public void run() {
        if (counter > 0) {
            PacketNPC.moveForwards(player);
            counter--;
        } else {
            this.cancel();
        }
    }
}
