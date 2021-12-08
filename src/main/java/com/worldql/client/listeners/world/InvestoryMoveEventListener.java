package com.worldql.client.listeners.world;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class InvestoryMoveEventListener implements Listener {

    @EventHandler
    public void onInventoryMoveEvent(InventoryMoveItemEvent e) {
        System.out.println("INVENTORY MOVE EVENT");
        e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryOpenEvent(InventoryOpenEvent e) {
        System.out.println("INVENTORY OPEN EVENT");
    }

    @EventHandler
    public void onInventoryCloseEvent(InventoryCloseEvent e) {
        System.out.println("INVENTORY CLOSE EVENT");
    }

    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent e) {
        System.out.println("INVENTORY CLICK EVENT");
    }
}
