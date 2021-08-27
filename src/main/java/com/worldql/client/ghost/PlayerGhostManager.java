package com.worldql.client.ghost;

import WorldQLFB.StandardEvents.Update;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.*;
import net.minecraft.server.level.PlayerInteractManager;
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

    public static void updateNPC(Update state) {

        // TODO: Make this faster.
        // Don't do packet tricks for local players
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (state.uuid().equals(player.getUniqueId().toString())) {
                // player is local
                return;
            }
        }

        UUID playerUUID = UUID.fromString(state.uuid());
        // Do we have this NPC in our expiring entity player?
        ExpiringEntityPlayer expiringEntityPlayer;
        if (hashtableNPCs.containsKey(playerUUID)) {
            expiringEntityPlayer = hashtableNPCs.get(playerUUID);
        } else {
            expiringEntityPlayer = PlayerGhostManager.createNPC(state.name(), playerUUID,
                    new Location(Bukkit.getServer().getWorld(Objects.requireNonNull(state.worldName())),
                            state.position().x(), state.position().y(), state.position().z()));
            sendNPCJoinPacket(expiringEntityPlayer.grab());
            hashtableNPCs.put(playerUUID, expiringEntityPlayer);
            integerNPCLookup.put(expiringEntityPlayer.grab().getId(), expiringEntityPlayer);
        }

        EntityPlayer e = expiringEntityPlayer.grab();

        if (state.instruction().equals("MinecraftPlayerQuit")) {
            sendNPCJoinPacket(e);
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
            Iterator ghostI = hashtableNPCs.entrySet().iterator();

            while (ghostI.hasNext()) {
                ExpiringEntityPlayer expiringEntityPlayer = (ExpiringEntityPlayer) ((Map.Entry) ghostI.next()).getValue();
                sendNPCJoinPacket(expiringEntityPlayer.grab(), p);
            }
            playerNeedsGhosts.put(p.getUniqueId(), false);

        }
    }

    public static void moveEntity(Update state, EntityPlayer e) {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            ensurePlayerHasJoinPackets(player);

            PlayerConnection connection = ((CraftPlayer) player).getHandle().b;
            e.setLocation(
                    state.position().x(),
                    state.position().y(),
                    state.position().z(),
                    state.yaw(),
                    state.pitch()
            );
            connection.sendPacket(
                    new PacketPlayOutEntityTeleport(e)
            );

            connection.sendPacket(new PacketPlayOutEntityHeadRotation(e, (byte) ((state.yaw() * 256) / 360)));

            for (int i = 0; i < state.entityactionsLength(); i++) {
                String action = state.entityactions(i);
                DataWatcher dw = new DataWatcher(null);
                if (action.equals("crouch")) {
                    dw.register(new DataWatcherObject<>(6, DataWatcherRegistry.s), EntityPose.f);
                    PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(e.getId(), dw, true);
                    connection.sendPacket(packet);
                } else if (action.equals("uncrouch")) {
                    dw.register(new DataWatcherObject<>(6, DataWatcherRegistry.s), EntityPose.a);
                    PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(e.getId(), dw, true);
                    connection.sendPacket(packet);
                }

                if (action.equals("punch")) {
                    PacketPlayOutAnimation punch = new PacketPlayOutAnimation(e, (byte) 0);
                    connection.sendPacket(punch);
                    //PacketPlayOutAnimation damage = new PacketPlayOutAnimation(e, (byte) 1);
                    //connection.sendPacket(damage);
                }
            }
            /*
            DataWatcher dw = new DataWatcher(null);
            WorldQLClient.logger.info(state.getAction());
            if (state.getAction().equals("crouch")) {
                dw.register(new DataWatcherObject<>(6, DataWatcherRegistry.s), EntityPose.CROUCHING);
                PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(e.getId(), dw, true);
                connection.sendPacket(packet);
            } else if (state.getAction().equals("uncrouch")) {
                dw.register(new DataWatcherObject<>(6, DataWatcherRegistry.s), EntityPose.STANDING);
                PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(e.getId(), dw, true);
                connection.sendPacket(packet);
            }

            if (state.getAction().equals("punch")) {
                PacketPlayOutAnimation punch = new PacketPlayOutAnimation(e, (byte) 0);
                connection.sendPacket(punch);
                //PacketPlayOutAnimation damage = new PacketPlayOutAnimation(e, (byte) 1);
                //connection.sendPacket(damage);
            }

             */
        }


    }

}
