package org.nqnl.mammothgameserver.events;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RemoteBlockChangeEvent extends Event {
    private final String data;

    public RemoteBlockChangeEvent(String data) {
        this.data = data;
    }

    private static final HandlerList HANDLERS = new HandlerList();

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public String getData() {
        return this.data;
    }
}
