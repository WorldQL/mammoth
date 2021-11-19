package com.worldql.client.protocols;

import com.worldql.client.ghost.ExpiringEntityPlayer;
import com.worldql.client.serialization.Message;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class MinecraftPlayerDamage {

    public static void process(Message state, Player player, ExpiringEntityPlayer e) {

        // proccess knockback and damage
        Bukkit.getLogger().info("Damage Received!");
    }
}
