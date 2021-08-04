package com.worldql.client.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.util.Vector;

public class OutgoingPlayerHitEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final int playerId;
    private final Vector direction;

    public OutgoingPlayerHitEvent(int playerId, Vector direction) {
        this.playerId = playerId;
        this.direction = direction;
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

    public Vector getDirection() {
        return this.direction;
    }

}
