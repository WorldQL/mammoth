package com.worldql.client.listeners;

import WorldQLFB.StandardEvents.Update;
import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.events.IncomingPlayerHitEvent;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerAnimationEvent;

public class IncomingPlayerHitListener implements Listener {

    @EventHandler
    public void onPlayerHit(IncomingPlayerHitEvent e) {
        System.out.println(e.getPlayerId());
        System.out.println("asdfasdf");
    }
}
