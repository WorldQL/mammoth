package org.nqnl.mammothgameserver.listeners;

import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.nqnl.mammothgameserver.MammothGameserver;
import org.nqnl.mammothgameserver.util.PlayerTransfer;

public class PlayerTeleportEventListener implements Listener {
    private static MammothGameserver instance;
    public PlayerTeleportEventListener(MammothGameserver _instance) {
        this.instance = _instance;
    }
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause().equals(PlayerTeleportEvent.TeleportCause.PLUGIN)) {
            PlayerTransfer.transferPlayerIfNeeded(event.getTo(), event, event.getPlayer(), instance);
        }
    }
}
