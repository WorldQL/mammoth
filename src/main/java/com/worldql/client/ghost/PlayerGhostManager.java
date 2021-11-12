package com.worldql.client.ghost;

import WorldQLFB_OLD.StandardEvents.Update;
import com.google.flatbuffers.FlexBuffers;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.worldql.client.Messages.Message;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.entity.EntityPose;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public class PlayerGhostManager {

    private static final Hashtable<UUID, ExpiringEntityPlayer> hashtableNPCs = new Hashtable<>();
    public static final Hashtable<UUID, Boolean> playerNeedsGhosts = new Hashtable<>();
    public static final Hashtable<Integer, ExpiringEntityPlayer> integerNPCLookup = new Hashtable<>();

    public static void updateNPC(Message state) {
        FlexBuffers.Map playerMessageMap = FlexBuffers.getRoot(state.flexAsByteBuffer()).asMap();

        UUID playerUUID = UUID.fromString(playerMessageMap.get("uuid").asString());
        // Do we have this NPC in our expiring entity player?
        ExpiringEntityPlayer expiringEntityPlayer;
        if (hashtableNPCs.containsKey(playerUUID)) {
            expiringEntityPlayer = hashtableNPCs.get(playerUUID);
        } else {
            expiringEntityPlayer = PlayerGhostManager.createNPC(playerMessageMap.get("username").asString(), playerUUID,
                    new Location(Bukkit.getServer().getWorld(Objects.requireNonNull(state.worldName())),
                            state.position().x(), state.position().y(), state.position().z()));
            sendNPCJoinPacket(expiringEntityPlayer.grab());
            hashtableNPCs.put(playerUUID, expiringEntityPlayer);
            integerNPCLookup.put(expiringEntityPlayer.grab().getId(), expiringEntityPlayer);
        }

        EntityPlayer e = expiringEntityPlayer.grab();

        if (state.parameter().equals("MinecraftPlayerQuit")) {
            sendNPCLeavePacket(e);
            int npcId = hashtableNPCs.get(playerUUID).grab().getId();
            hashtableNPCs.remove(playerUUID);
            integerNPCLookup.remove(npcId);
            playerNeedsGhosts.remove(playerUUID);
            return;
        }

        moveEntity(state, e);
    }

    private static ExpiringEntityPlayer createNPC(String name, UUID uuid, Location location) {

        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        WorldServer world = ((CraftWorld) location.getWorld()).getHandle();
        GameProfile profile = new GameProfile(uuid, name);
        EntityPlayer npc = new EntityPlayer(server, world, profile);
        npc.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

        //String[] skinData = getSkin(uuid);
        String[] skinData = {"", ""};
        profile.getProperties().put("textures",
                new Property("textures", skinData[0], skinData[1])
        );

        return new ExpiringEntityPlayer(npc);
    }

    private static void sendNPCLeavePacket(EntityPlayer npc) {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            PlayerConnection connection = ((CraftPlayer) player).getHandle().b;
            connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.e, npc));
            connection.sendPacket(new PacketPlayOutEntityDestroy(npc.getId()));
        }
    }

    private static void sendNPCJoinPacket(EntityPlayer npc) {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            PlayerConnection connection = ((CraftPlayer) player).getHandle().b;
            connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.a, npc));
            connection.sendPacket(new PacketPlayOutNamedEntitySpawn(npc));
            connection.sendPacket(new PacketPlayOutEntityHeadRotation(npc, (byte) ((npc.getYRot() * 256) / 360)));
        }
    }

    private static void sendNPCJoinPacket(EntityPlayer npc, Player player) {
        PlayerConnection connection = ((CraftPlayer) player).getHandle().b;
        connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.a, npc));
        connection.sendPacket(new PacketPlayOutNamedEntitySpawn(npc));
        connection.sendPacket(new PacketPlayOutEntityHeadRotation(npc, (byte) ((npc.getYRot() * 256) / 360)));
    }

    // useless, teleport packet can be used for everything.
    private static short computeMovementDelta(float current, float previous) {
        return (short) ((short) (current * 32 - previous * 32) * 128);
    }

    // TODO: This does not load the outer layer of the skin.
    private static String[] getSkin(UUID uuid) throws IllegalArgumentException {
        URL url = null;
        try {
            url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
        } catch (Exception e) {
            e.printStackTrace();
        }
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        // get player skin blob and signature in hex.
        try {
            JsonObject prop = new JsonParser().parse(reader).getAsJsonObject().get("properties").getAsJsonArray().get(
                    0).getAsJsonObject();
            String texture = prop.get("value").getAsString();
            String signature = prop.get("signature").getAsString();
            return new String[]{texture, signature};
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException();
        }

    }

    private static void ensurePlayerHasJoinPackets(Player p) {
        if (playerNeedsGhosts.containsKey(p.getUniqueId()) && playerNeedsGhosts.get(p.getUniqueId())) {
            // Spawn ghosts for this player
            for (Map.Entry<UUID, ExpiringEntityPlayer> uuidExpiringEntityPlayerEntry : hashtableNPCs.entrySet()) {
                ExpiringEntityPlayer expiringEntityPlayer = (ExpiringEntityPlayer) ((Map.Entry) uuidExpiringEntityPlayerEntry).getValue();
                sendNPCJoinPacket(expiringEntityPlayer.grab(), p);
            }
            playerNeedsGhosts.put(p.getUniqueId(), false);

        }
    }

    public static void moveEntity(Message state, EntityPlayer e) {
        FlexBuffers.Map playerMessageMap = FlexBuffers.getRoot(state.flexAsByteBuffer()).asMap();
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            ensurePlayerHasJoinPackets(player);
            PlayerConnection connection = ((CraftPlayer) player).getHandle().b;
            if (Objects.equals(state.parameter(), "MinecraftPlayerMove")) {
                float playerYaw = (float) playerMessageMap.get("yaw").asFloat();
                e.setLocation(
                        state.position().x(),
                        state.position().y(),
                        state.position().z(),
                        playerYaw,
                        (float) playerMessageMap.get("pitch").asFloat()
                );
                connection.sendPacket(
                        new PacketPlayOutEntityTeleport(e)
                );
                connection.sendPacket(new PacketPlayOutEntityHeadRotation(e, (byte) ((playerYaw * 256) / 360)));
            }
            // We use MinecraftPlayerSingleAction for punch, crouch, and uncrouch
            if (Objects.equals(state.parameter(), "MinecraftPlayerSingleAction")) {
                String action = playerMessageMap.get("action").asString();
                DataWatcher dw = new DataWatcher(null);
                if (action.equals("crouch")) {
                    dw.register(new DataWatcherObject<>(6, DataWatcherRegistry.s), EntityPose.f);
                    PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(e.getId(), dw, true);
                    connection.sendPacket(packet);
                }
                if (action.equals("uncrouch")) {
                    dw.register(new DataWatcherObject<>(6, DataWatcherRegistry.s), EntityPose.a);
                    PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(e.getId(), dw, true);
                    connection.sendPacket(packet);
                }
                if (action.equals("punch")) {
                    PacketPlayOutAnimation punch = new PacketPlayOutAnimation(e, (byte) 0);
                    connection.sendPacket(punch);
                }
            }
        }


    }

}
