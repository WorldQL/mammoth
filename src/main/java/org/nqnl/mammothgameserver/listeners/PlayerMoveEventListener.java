package org.nqnl.mammothgameserver.listeners;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.nqnl.mammothgameserver.MammothGameserver;
import org.nqnl.mammothgameserver.util.PlayerTransfer;

public class PlayerMoveEventListener implements Listener {

    private static MammothGameserver instance;
    public PlayerMoveEventListener(MammothGameserver _instance) {
        this.instance = _instance;
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        Location to = event.getTo();
        PlayerTransfer.transferPlayerIfNeeded(to, event, event.getPlayer(), this.instance);
    }
}
