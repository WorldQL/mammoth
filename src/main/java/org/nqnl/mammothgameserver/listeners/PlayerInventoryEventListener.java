package org.nqnl.mammothgameserver.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.nqnl.mammothgameserver.MammothGameserver;
import org.nqnl.mammothgameserver.util.PlayerDataSerialize;
import redis.clients.jedis.Jedis;

public class PlayerInventoryEventListener implements Listener {
    private static MammothGameserver instance;
    public PlayerInventoryEventListener(MammothGameserver _instance) {
        this.instance = _instance;
    }

    @EventHandler
    public void onEnderChestOpen(InventoryOpenEvent e) {
        if (e.getInventory().getType().equals(InventoryType.ENDER_CHEST)) {
            Jedis j = null;
            try {
                j = MammothGameserver.pool.getResource();
                if (j.exists("player-" + e.getPlayer().getUniqueId().toString() + "-enderchest")) {
                    String data = j.get("player-" + e.getPlayer().getUniqueId().toString() + "-enderchest");
                    ItemStack[] contents = PlayerDataSerialize.itemStackArrayFromBase64(data);
                    e.getInventory().setContents(contents);
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            } finally {
                j.close();
            }
        }
    }
    @EventHandler
    public void onEnderChestClose(InventoryCloseEvent event) {
        if (event.getInventory().getType().equals(InventoryType.ENDER_CHEST)) {
            String serializedInventory = PlayerDataSerialize.itemStackArrayToBase64(event.getInventory().getContents());
            Jedis j = null;
            try {
                j = MammothGameserver.pool.getResource();
                j.set("player-" + event.getPlayer().getUniqueId().toString() + "-enderchest", serializedInventory);
            } catch (Exception exception) {
                exception.printStackTrace();
            } finally {
                j.close();
            }
        }
    }

}
