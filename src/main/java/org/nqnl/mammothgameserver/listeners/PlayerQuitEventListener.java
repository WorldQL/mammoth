package org.nqnl.mammothgameserver.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nqnl.mammothgameserver.MammothGameserver;
import org.nqnl.mammothgameserver.util.ServerTransferPayload;
import redis.clients.jedis.Jedis;

public class PlayerQuitEventListener implements Listener {
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Jedis j = null;
        j = MammothGameserver.pool.getResource();
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
