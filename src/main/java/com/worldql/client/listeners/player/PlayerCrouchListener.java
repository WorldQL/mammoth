package com.worldql.client.listeners.player;

import com.worldql.client.listeners.utils.OutgoingMinecraftPlayerSingleAction;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class PlayerCrouchListener implements Listener {
    @EventHandler
    public void onCrouch(PlayerToggleSneakEvent e) {
        Location playerLocation = e.getPlayer().getLocation();
        String action = e.isSneaking() ? "crouch" : "uncrouch";
        OutgoingMinecraftPlayerSingleAction.sendPacket(playerLocation, e.getPlayer(), action);
    }
}
