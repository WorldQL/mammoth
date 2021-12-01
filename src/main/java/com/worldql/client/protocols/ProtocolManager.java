package com.worldql.client.protocols;

import com.worldql.client.WorldQLClient;
import com.worldql.client.events.OutgoingPlayerHitEvent;
import com.worldql.client.ghost.ExpiringEntityPlayer;
import com.worldql.client.ghost.PlayerGhostManager;
import io.netty.channel.Channel;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.EntityHuman;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_18_R1.CraftServer;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class ProtocolManager {

    private static Protocol protocol;

    public static void read() {
        protocol = new Protocol(WorldQLClient.getPluginInstance()) {
            /***
             *
             * @param sender - the player that sent the packet, NULL for early login/status packets.
             * @param channel - channel that received the packet. Never NULL.
             * @param packet - the packet being received.
             * @return The packet to send instead, or NULL to cancel the transmission.
             */
            @Override
            public Object onPacketInAsync(Player sender, Channel channel, Object packet) {
                if (packet instanceof PacketPlayInUseEntity) {
                    int playerId = (int) getValue(packet, "a");

                    // lazy solution until another one is found (this will only get the attack action)
                    if (getValue(packet, "b").toString().split("\\$")[1].charAt(0) == '1') {
                        ExpiringEntityPlayer entity = PlayerGhostManager.integerNPCLookup.get(playerId);
                        PlayerConnection connection = ((CraftPlayer) sender).getHandle().b;
                        if (entity != null) {
                            Bukkit.getScheduler().runTask(WorldQLClient.getPluginInstance(),
                                    () -> Bukkit.getPluginManager().callEvent(new OutgoingPlayerHitEvent(sender, entity.grab())));

                            connection.a(new PacketPlayOutAnimation(entity.grab(), (byte) 1));
                        }
                    }
                }
                return super.onPacketInAsync(sender, channel, packet);
            }

            /***
             *
             * <p>
             * @param receiver - the receiving player, NULL for early login/status packets.
             * @param channel - the channel that received the packet. Never NULL.
             * @param packet - the packet being sent.
             * @return The packet to send instead, or NULL to cancel the transmission.
             */
            @Override
            public Object onPacketOutAsync(Player receiver, Channel channel, Object packet) {
                return super.onPacketOutAsync(receiver, channel, packet);
            }
        };
    }

    public static void injectPlayer(Player player) {
        protocol.injectPlayer(player);
    }


    public static void uninjectPlayer(Player player) {
        protocol.uninjectPlayer(player);
    }

    public static boolean isinjected(Player player) {
        return protocol.hasInjected(player);
    }


    public static void sendGenericPacket(Packet<?> packet) {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            PlayerConnection connection = ((CraftPlayer) player).getHandle().b;
            connection.a(packet);
        }
    }

    public static void sendLeavePacket(EntityPlayer npc) {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            PlayerConnection connection = ((CraftPlayer) player).getHandle().b;
            connection.a(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.e, npc));
            connection.a(new PacketPlayOutEntityDestroy(npc.ae()));
        }
    }

    public static void sendJoinPacket(EntityPlayer npc) {
        // this is for the overlay skin data
        DataWatcher watcher = npc.ai();
        byte b = 0x01 | 0x02 | 0x04 | 0x08 | 0x10 | 0x20 | 0x40;
        watcher.a(DataWatcherRegistry.a.a(17), b);
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            PlayerConnection connection = ((CraftPlayer) player).getHandle().b;
            connection.a(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.a, npc));
            connection.a(new PacketPlayOutNamedEntitySpawn(npc));
            connection.a(new PacketPlayOutEntityHeadRotation(npc, (byte) ((npc.getBukkitYaw() * 256) / 360)));
            connection.a(new PacketPlayOutEntityMetadata(npc.ae(), watcher, true));
        }
    }

    public static void sendJoinPacket(EntityPlayer npc, Player player) {
        DataWatcher watcher = npc.ai();
        byte b = 0x01 | 0x02 | 0x04 | 0x08 | 0x10 | 0x20 | 0x40;
        watcher.a(DataWatcherRegistry.a.a(17), b);
        PlayerConnection connection = ((CraftPlayer) player).getHandle().b;
        connection.a(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.a, npc));
        connection.a(new PacketPlayOutNamedEntitySpawn(npc));
        connection.a(new PacketPlayOutEntityHeadRotation(npc, (byte) ((npc.getBukkitYaw() * 256) / 360)));
        connection.a(new PacketPlayOutEntityMetadata(npc.ae(), watcher, true));
    }
}
