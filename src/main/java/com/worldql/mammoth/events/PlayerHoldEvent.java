package com.worldql.mammoth.events;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerHoldEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final ItemStack piece;
    private final HandType type;

    public PlayerHoldEvent(Player who, ItemStack piece, HandType type) {
        super(who);
        this.piece = piece;
        this.type = type;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public ItemStack getPiece() {
        if (piece == null)
            return new ItemStack(Material.AIR);
        return piece;
    }

    public HandType getType() {
        return type;
    }

    public enum HandType {
        MAINHAND,
        OFFHAND;
    }
}
