package org.nqnl.mammothgameserver.listeners;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nqnl.mammothgameserver.events.RemoteBlockChangeEvent;

import java.util.HashMap;
import java.util.Map;

import static org.nqnl.mammothgameserver.util.PlayerTransfer.STARTING_PORT;

public class RemoteBlockChangeEventListener implements Listener {

    @EventHandler
    public void onRemoteBlockChange(RemoteBlockChangeEvent event) {
        String data = event.getData();
        try {
            HashMap<String, Object> blockData = new HashMap<String, Object>();
            ObjectMapper mapper = new ObjectMapper();
            blockData = mapper.readValue(data, new TypeReference<Map<String, Object>>(){});
            // if ours
            if (blockData.get("target").equals(Bukkit.getServer().getPort() - STARTING_PORT)) {
                if (blockData.get("action").equals("place")) {
                    Bukkit.getServer().getWorld((String)blockData.get("world")).getBlockAt((Integer) blockData.get("x"), (Integer) blockData.get("y"), (Integer) blockData.get("z")).setBlockData(Bukkit.getServer().createBlockData((String)blockData.get("data")));
                } else if (blockData.get("action").equals("break")) {
                    Bukkit.getServer().getWorld((String)blockData.get("world")).getBlockAt((Integer) blockData.get("x"), (Integer) blockData.get("y"), (Integer) blockData.get("z")).setType(Material.AIR);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


