package com.worldql.client.ghost;

import com.google.flatbuffers.FlexBuffers;
import com.mojang.authlib.GameProfile;
import com.worldql.client.protocols.*;
import com.worldql.client.serialization.Message;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R1.CraftServer;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;

import java.util.*;

public class PlayerGhostManager {

    private static final Hashtable<UUID, ExpiringEntityPlayer> hashtableNPCs = new Hashtable<>();
    public static final Hashtable<Integer, ExpiringEntityPlayer> integerNPCLookup = new Hashtable<>();

    public static void updateNPC(Message state) {
        FlexBuffers.Map playerMessageMap = FlexBuffers.getRoot(state.flex()).asMap();

        UUID playerUUID = UUID.fromString(playerMessageMap.get("uuid").asString());

        if (state.parameter().equals("MinecraftPlayerDamage")) {
            MinecraftPlayerDamage.process(state, Bukkit.getPlayer(playerUUID), hashtableNPCs.get(UUID.fromString(playerMessageMap.get("uuidofattacker").asString())));
            return;
        }
        // TODO maybe a better design for this?
        ExpiringEntityPlayer expiringEntityPlayer;
        if (hashtableNPCs.containsKey(playerUUID))
            expiringEntityPlayer = hashtableNPCs.get(playerUUID);
        else {
            expiringEntityPlayer = PlayerGhostManager.createNPC(playerMessageMap.get("username").asString(), playerUUID,
                    new Location(Bukkit.getServer().getWorld(Objects.requireNonNull(state.worldName())),
                            state.position().x(), state.position().y(), state.position().z()));
            ProtocolManager.sendJoinPacket(expiringEntityPlayer.grab());
            hashtableNPCs.put(playerUUID, expiringEntityPlayer);
            integerNPCLookup.put(expiringEntityPlayer.grab().ae(), expiringEntityPlayer);
        }

        EntityPlayer entity = expiringEntityPlayer.grab();

        if (state.parameter().equals("MinecraftPlayerQuit")) {
            ProtocolManager.sendLeavePacket(entity);
            int npcId = hashtableNPCs.get(playerUUID).grab().ae();
            hashtableNPCs.remove(playerUUID);
            integerNPCLookup.remove(npcId);
            return;
        }
        processPacket(state, entity);
    }

    /***
     * This gets the UUID of a player from its entity on the other server
     * @param id - entity id
     * @return - the uuid of the player it's mimicking
     */
    public static UUID getUUIDfromID(int id) {
        UUID uuid = null;
        ExpiringEntityPlayer eep = integerNPCLookup.get(id);
        for (Map.Entry<UUID, ExpiringEntityPlayer> h : hashtableNPCs.entrySet()) {
            if (h.getValue().equals(eep))
                uuid = h.getKey();
        }
        return uuid;
    }


    private static ExpiringEntityPlayer createNPC(String name, UUID uuid, Location location) {
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        WorldServer world = ((CraftWorld) location.getWorld()).getHandle();
        GameProfile profile = new GameProfile(uuid, name);
        EntityPlayer npc = new EntityPlayer(server, world, profile);
        npc.a(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        return new ExpiringEntityPlayer(npc);
    }

    public static void ensurePlayerHasJoinPackets(UUID p) {
        // Spawn ghosts for this player
        for (Map.Entry<UUID, ExpiringEntityPlayer> uuidExpiringEntityPlayerEntry : hashtableNPCs.entrySet()) {
            ExpiringEntityPlayer expiringEntityPlayer = uuidExpiringEntityPlayerEntry.getValue();
            ProtocolManager.sendJoinPacket(expiringEntityPlayer.grab(), Bukkit.getPlayer(p));
        }
    }

    public static void processPacket(Message state, EntityPlayer entity) {
        switch (state.parameter()) {
            case "MinecraftPlayerMove" -> MinecraftPlayerMove.process(state, entity);
            case "MinecraftPlayerSingleAction" -> MinecraftPlayerSingleAction.process(state, entity);
            case "MinecraftPlayerEquipmentEdit" -> MinecraftPlayerEquipmentEdit.process(state, entity);
            case "MinecraftPlayerShieldUse" -> MinecraftPlayerShieldUse.process(state, entity);
            case "MinecraftPlayerShootBow" -> MinecraftPlayerShootBow.process(state, entity);
        }
    }

}
