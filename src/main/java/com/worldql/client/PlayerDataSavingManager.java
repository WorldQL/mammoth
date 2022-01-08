package com.worldql.client;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

public class PlayerDataSavingManager {
    private final HashMap<UUID, Long> timeSinceLastSave;
    private final HashMap<UUID, Boolean> syncedAfterJoin;
    private final HashMap<UUID, Long> playerLoginTime;

    public PlayerDataSavingManager() {
        timeSinceLastSave = new HashMap<>();
        syncedAfterJoin = new HashMap<>();
        playerLoginTime = new HashMap<>();
    }

    public void recordLogin(Player p) {
        long now = Instant.now().toEpochMilli();
        playerLoginTime.put(p.getUniqueId(), now);
    }

    public void processLogout(Player p) {
        UUID u = p.getUniqueId();
        timeSinceLastSave.remove(u);
        syncedAfterJoin.remove(u);
        playerLoginTime.remove(u);
    }

    public long getMsSinceLogin(Player p) {
        UUID playerUUID = p.getUniqueId();
        if (!playerLoginTime.containsKey(playerUUID)) {
            return 0;
        }
        long now = Instant.now().toEpochMilli();
        return now - playerLoginTime.get(playerUUID);
    }

    public void markSaved(Player p) {
        long now = Instant.now().toEpochMilli();
        timeSinceLastSave.put(p.getUniqueId(), now);
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
    public boolean isFullySynced(Player p) {
        if (!syncedAfterJoin.containsKey(p.getUniqueId())) {
            return false;
        }
        return syncedAfterJoin.get(p.getUniqueId());
    }

    public void markUnsynced(Player p) {
        syncedAfterJoin.put(p.getUniqueId(), false);
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 250, 10));
    }

    public void markSafe(Player p) {
        syncedAfterJoin.put(p.getUniqueId(), true);
    }

}
