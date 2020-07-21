package org.nqnl.mammothgameserver.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nqnl.mammothgameserver.MammothGameserver;
import org.nqnl.mammothgameserver.util.ServerTransferPayload;
import redis.clients.jedis.Jedis;

import static org.nqnl.mammothgameserver.util.PlayerTransfer.STARTING_PORT;

public class PlayerQuitEventListener implements Listener {
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage("");
        Jedis j = null;
        j = MammothGameserver.pool.getResource();
        j.set(Bukkit.getServer().getPort() - STARTING_PORT + "-playercount", Integer.toString(Bukkit.getServer().getOnlinePlayers().size() - 1));
        try {
            if (!j.exists("cooldown-" + event.getPlayer().getUniqueId().toString())) {
                String playerAsJson = ServerTransferPayload.createPayload(event.getPlayer());
                j.set("player-" + event.getPlayer().getUniqueId().toString(), playerAsJson);
            }
        } finally {
            j.close();
        }
    }
}
