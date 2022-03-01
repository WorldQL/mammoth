package com.worldql.mammoth.listeners.player;

import com.worldql.mammoth.MinecraftUtil;
import com.worldql.mammoth.MammothPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import redis.clients.jedis.Jedis;

public class PlayerInventoryOpenEventListener implements Listener {
    @EventHandler
    public void onEnderChestOpen(InventoryOpenEvent e) {
        if (e.getInventory().getType().equals(InventoryType.ENDER_CHEST)) {
            try (Jedis j = MammothPlugin.pool.getResource()) {
                if (j.exists("player-" + e.getPlayer().getUniqueId().toString() + "-enderchest")) {
                    String data = j.get("player-" + e.getPlayer().getUniqueId().toString() + "-enderchest");
                    ItemStack[] contents = MinecraftUtil.itemStackArrayFromBase64(data);
                    e.getInventory().setContents(contents);
                }
            } catch (Exception exception) {
                exception.printStackTrace();;
            }
        }
    }
    @EventHandler
    public void onEnderChestClose(InventoryCloseEvent event) {
        if (event.getInventory().getType().equals(InventoryType.ENDER_CHEST)) {
            String serializedInventory = MinecraftUtil.itemStackArrayToBase64(event.getInventory().getContents());
            try (Jedis j = MammothPlugin.pool.getResource()) {
                j.set("player-" + event.getPlayer().getUniqueId().toString() + "-enderchest", serializedInventory);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

}
