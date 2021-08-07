package com.worldql.client.incoming;

import WorldQLFB.StandardEvents.Update;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Objects;

public class PlayerHit {
    public static void process(Update update, Plugin plugin) {
        String playerId = update.params(0);
        System.out.println(playerId);
        // TODO: Make this faster
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (Objects.equals(player.getUniqueId().toString(), playerId)) {
                System.out.println(player.getDisplayName());
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        System.out.println("attempting to set velocity");
                        player.setVelocity(player.getVelocity().add(new Vector(
                                        update.numericalParams(0),
                                        update.numericalParams(1),
                                        update.numericalParams(2)
                                ).multiply(0.8).add(new Vector(0, 0.5, 0))
                        ));
                    }
                }.runTask(plugin);
                return;
            }
        }

    }
}
