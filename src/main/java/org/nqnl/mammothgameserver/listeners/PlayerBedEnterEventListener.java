package org.nqnl.mammothgameserver.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.nqnl.mammothgameserver.MammothGameserver;
import redis.clients.jedis.Jedis;

import java.util.HashMap;

public class PlayerBedEnterEventListener implements Listener {
    @EventHandler
    public void onPlayerEnterBed(PlayerBedEnterEvent event) {
        event.setCancelled(true);
        Location bedLocation = event.getBed().getLocation();
        HashMap<String, Object> bedEventData = new HashMap<String, Object>();
        bedEventData.put("bedX", bedLocation.getX());
        bedEventData.put("bedY", bedLocation.getY());
        bedEventData.put("bedZ", bedLocation.getZ());
        ObjectMapper mapper = new ObjectMapper();
        Jedis j = MammothGameserver.pool.getResource();
        try {
            String bedEventJson = mapper.writeValueAsString(bedEventData);
            j.set("bed-"+event.getPlayer().getUniqueId().toString(), bedEventJson);
            event.getPlayer().sendMessage("Bed set! Use /removebed to remove your respawn point.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            j.close();
        }
    }
}
