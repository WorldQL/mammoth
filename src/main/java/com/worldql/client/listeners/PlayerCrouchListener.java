package com.worldql.client.listeners;

import com.worldql.client.listeners.utils.PlayerMoveUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class PlayerCrouchListener implements Listener {
    @EventHandler
    public void onCrouch(PlayerToggleSneakEvent event) {
        PlayerMoveUtils.sendPacket(
                event.getPlayer().getLocation(),
                event.getPlayer(),
                new String[]{event.isSneaking() ? "crouch" : "uncrouch"}
        );
    }
}
