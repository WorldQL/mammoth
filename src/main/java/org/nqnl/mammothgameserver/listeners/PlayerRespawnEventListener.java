package org.nqnl.mammothgameserver.listeners;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.nqnl.mammothgameserver.MammothGameserver;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.nqnl.mammothgameserver.util.PlayerTransfer.STARTING_PORT;

public class PlayerRespawnEventListener implements Listener {

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Jedis j = MammothGameserver.pool.getResource();
        try {
            if (j.exists("bed-"+event.getPlayer().getUniqueId().toString())) {
                HashMap<String, Object> bedData = new HashMap<String, Object>();
                ObjectMapper mapper = new ObjectMapper();
                bedData = mapper.readValue((String)j.get("bed-"+event.getPlayer().getUniqueId().toString()), new TypeReference<Map<String, Object>>(){});
                Location l = new Location(Bukkit.getWorld("world"), (Double) bedData.get("bedX"), (Double) bedData.get("bedY"), (Double) bedData.get("bedZ"));
                event.setRespawnLocation(l);
                j.set("dead-"+event.getPlayer().getUniqueId(), "true");
                j.expire("dead-"+event.getPlayer().getUniqueId(), 15);
            } else {
                // no bed, attempts to choose a random spawn location on the server's slice.
                // caveat: all spawn locations have a positive x. whatever.
                Random rd = new Random();
                int currentServer = Bukkit.getServer().getPort() - STARTING_PORT;
                int spawnX = 1024 * currentServer + 200 + rd.nextInt(400);
                int spawnZ = rd.nextInt(2000) - rd.nextInt(2000);
                Location spawn = new Location(Bukkit.getWorld("world"), spawnX, 65, spawnZ);
                event.setRespawnLocation(spawn);

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            j.close();
        }
    }
}
