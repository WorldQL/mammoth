package com.worldql.client.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class OutgoingPlayerHitEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final int playerId;

    public OutgoingPlayerHitEvent(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public int getPlayerId() {
        return this.playerId;
    }

}
