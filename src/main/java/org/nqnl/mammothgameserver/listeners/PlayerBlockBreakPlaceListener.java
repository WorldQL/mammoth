package org.nqnl.mammothgameserver.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.nqnl.mammothgameserver.MammothGameserver;
import org.nqnl.mammothgameserver.SliceMethods;
import org.nqnl.mammothgameserver.util.PlayerTransfer;
import redis.clients.jedis.Jedis;

import java.util.HashMap;

public class PlayerBlockBreakPlaceListener implements Listener {
    private static MammothGameserver instance;

    public PlayerBlockBreakPlaceListener(MammothGameserver _instance) {
        this.instance = _instance;
    }
    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent event) {
        if (PlayerTransfer.transferPlayerIfNeeded(event.getBlockPlaced().getLocation(), event, event.getPlayer(), instance)) {
            return;
        }
        if (event.getBlockPlaced().getLocation().getWorld().getName().equals("world_the_end")) {
            return;
        }
        if (SliceMethods.getDMZStatus(event.getBlockPlaced().getLocation().getX())) {
            Jedis j = null;
            int targetNeighbor = SliceMethods.getNearestNeighbor(event.getBlockPlaced().getLocation().getX());
            if (targetNeighbor == -1) return;

            try {
                j = MammothGameserver.pool.getResource();
                HashMap<String, Object> block = new HashMap<String, Object>();
                block.put("target", targetNeighbor);
                block.put("x", event.getBlock().getX());
                block.put("y", event.getBlock().getY());
                block.put("z", event.getBlock().getZ());
                block.put("action", "place");
                block.put("data", event.getBlock().getBlockData().getAsString());
                block.put("world", event.getBlock().getWorld().getName());
                ObjectMapper mapper = new ObjectMapper();
                String eventJson = mapper.writeValueAsString(block);
                j.publish("blockevents", eventJson);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                j.close();
            }
        }

    }
    @EventHandler
    public void onBreakBlock(BlockBreakEvent event) {
        if (PlayerTransfer.transferPlayerIfNeeded(event.getBlock().getLocation(), event, event.getPlayer(), instance)) {
            return;
        }
        if (event.getBlock().getLocation().getWorld().getName().equals("world_the_end")) {
            return;
        }
        if (SliceMethods.getDMZStatus(event.getBlock().getLocation().getX())) {
            Jedis j = null;
            int targetNeighbor = SliceMethods.getNearestNeighbor(event.getBlock().getLocation().getX());
            if (targetNeighbor == -1) return;

            try {
                j = MammothGameserver.pool.getResource();
                HashMap<String, Object> block = new HashMap<String, Object>();
                block.put("target", targetNeighbor);
                block.put("x", event.getBlock().getX());
                block.put("y", event.getBlock().getY());
                block.put("z", event.getBlock().getZ());
                block.put("action", "break");
                block.put("world", event.getBlock().getWorld().getName());
                ObjectMapper mapper = new ObjectMapper();
                String eventJson = mapper.writeValueAsString(block);
                j.publish("blockevents", eventJson);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                j.close();
            }
        }

    }
}
