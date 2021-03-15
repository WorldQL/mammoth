package com.worldql.client.ghost;

// This implements a version of entity player which stores the last time it was updated / accessed
// This is used so the server can clean up unused EntityPlayers

import net.minecraft.server.v1_16_R3.EntityPlayer;

import java.time.Instant;

public class ExpiringEntityPlayer {
    private long lastAccessed;
    private EntityPlayer entityPlayer;

    public ExpiringEntityPlayer(EntityPlayer e) {
        this.lastAccessed = Instant.now().getEpochSecond();
        this.entityPlayer = e;
    }

    public EntityPlayer grab() {
        lastAccessed = Instant.now().getEpochSecond();
        return entityPlayer;
    }

    public boolean shouldExpire() {
        long difference = Instant.now().getEpochSecond() - lastAccessed;
        if (difference > 120) {
            return true;
        } else {
            return false;
        }
    }
}
