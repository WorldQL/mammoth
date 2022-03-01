package com.worldql.mammoth.ghost;

// This implements a version of entity player which stores the last time it was updated / accessed
// This is used so the server can clean up unused EntityPlayers

import net.minecraft.server.level.EntityPlayer;

import java.time.Instant;

// TODO: Actually implement this class and allow entities to expire.
/* The use case for this is players walking outside of the subscription range of a server.
They won't get a nice clean PlayerDisconnectEvent. They will simply stop getting updated. We should create a scheduled
task which loops over the entity players and prunes expired ones.
 */
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
