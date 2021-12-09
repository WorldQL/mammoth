package com.worldql.client.listeners.explosions;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExplosionPrimeEvent;

public class ExplosionPrimeEventListener implements Listener {
    @EventHandler
    public void onExplosionPrime(ExplosionPrimeEvent e) {
        System.out.println(e.getRadius());
        // TODO: Use this to send cross server explosions instead of EntityExplodeEvent as this gives access to radius.
        
    }
}
