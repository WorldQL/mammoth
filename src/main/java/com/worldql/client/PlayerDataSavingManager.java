package com.worldql.client;

import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

public class PlayerDataSavingManager {
    private final HashMap<UUID, Long> timeSinceLastSave;
    private final HashMap<UUID, Boolean> syncedAfterJoin;

    public PlayerDataSavingManager() {
        timeSinceLastSave = new HashMap<>();
        syncedAfterJoin = new HashMap<>();
    }

    public void markSaved(Player p) {
        Instant now = Instant.now();
        timeSinceLastSave.put(p.getUniqueId(), now.toEpochMilli());
    }

    // Should we skip syncing this player? Useful if we don't want duplicates firing or bad player saves from other servers.
    public boolean skip(Player p, long thresholdMs) {
        if (!timeSinceLastSave.containsKey(p.getUniqueId())) {
            return false;
        }
        long now = Instant.now().toEpochMilli();
        long lastSave = timeSinceLastSave.get(p.getUniqueId());
        long diff = now - lastSave;
        return diff < thresholdMs;
    }

    // Can this player move and drop items?
    public boolean isSafe(Player p) {
        if (!syncedAfterJoin.containsKey(p.getUniqueId())) {
            return false;
        }
        return syncedAfterJoin.get(p.getUniqueId());
    }
    public void markUnsafe(Player p) {
        syncedAfterJoin.put(p.getUniqueId(), false);
    }
    public void markSafe(Player p) {
        syncedAfterJoin.put(p.getUniqueId(), true);
    }

}
