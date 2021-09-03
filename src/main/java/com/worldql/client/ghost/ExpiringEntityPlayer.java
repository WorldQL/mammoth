package com.worldql.client.ghost;

// This implements a version of entity player which stores the last time it was updated / accessed
// This is used so the server can clean up unused EntityPlayers

import net.minecraft.server.level.EntityPlayer;

import java.time.Instant;

public class ExpiringEntityPlayer {
    private long lastAccessed;
    private final EntityPlayer entityPlayer;

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
        return difference > 120;
    }
}
