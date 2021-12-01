package com.worldql.client.events;

import com.worldql.client.ghost.PlayerGhostManager;
import net.minecraft.server.level.EntityPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class OutgoingPlayerHitEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final EntityPlayer receiver;
    private final Player attacker;

    private final UUID uuid; //this is the uuid of the entity attacked

    // TODO Add attackers equipment/tools, add receivers equipment, add vector of attacker
    public OutgoingPlayerHitEvent(Player attacker, EntityPlayer receiver) {
        this.receiver = receiver;
        this.attacker = attacker;

        // this is used to send to the other servers.
        uuid = PlayerGhostManager.getUUIDfromID(receiver.ae());
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public EntityPlayer getReceiver() {
        return receiver;
    }

//    public double getDamage() {
//        return damage;
//    }


    public UUID getUUID() {
        return uuid;
    }

    public Player getAttacker() {
        return attacker;
    }
}
