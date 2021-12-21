package com.worldql.client.listeners.player;

import com.google.common.base.Charsets;
import com.google.flatbuffers.FlexBuffers;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.listeners.utils.ItemTools;
import com.worldql.client.protocols.ProtocolManager;
import com.worldql.client.worldql_serialization.*;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import zmq.ZMQ;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class PlayerLogOutListener implements Listener {
    @EventHandler
    public void onPlayerLogOut(PlayerQuitEvent e) {
        if (ProtocolManager.isinjected(e.getPlayer()))
            ProtocolManager.uninjectPlayer(e.getPlayer());

        // Send quit event to other clients
        FlexBuffersBuilder b = Codec.getFlexBuilder();
        int pmap = b.startMap();
        b.putString("username", e.getPlayer().getName());
        b.putString("uuid", e.getPlayer().getUniqueId().toString());
        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();

        Message message = new Message(
                Instruction.LocalMessage,
                WorldQLClient.worldQLClientId,
                e.getPlayer().getWorld().getName(),
                Replication.ExceptSelf,
                new Vec3D(e.getPlayer().getLocation()),
                null,
                null,
                "MinecraftPlayerQuit",
                bb
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
        // Save player position and inventory
        Player player = e.getPlayer();
        savePlayerToRedis(player);
    }

    @EventHandler
    public void onPlayerLogIn(PlayerJoinEvent e) {
        //String data = WorldQLClient.pool.getResource().get("player-" + e.getPlayer().getUniqueId());
        //setInventory(data, e.getPlayer().getUniqueId());
    }


    public static ByteBuffer str_to_bb(String msg){
        return ByteBuffer.wrap(msg.getBytes(Charsets.UTF_8));
    }

    public static String bb_to_str(ByteBuffer buffer){
        byte[] bytes;
        if(buffer.hasArray()) {
            bytes = buffer.array();
        } else {
            bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
        }
        return new String(bytes, Charsets.UTF_8);
    }


    public static void savePlayerToRedis(Player player) {
        FlexBuffersBuilder b = Codec.getFlexBuilder();
        int imap = b.startMap();
        b.putBlob("inventory", ItemTools.serializeItemStack(player.getInventory().getContents()));
        b.putBlob("echest", ItemTools.serializeItemStack(player.getEnderChest().getContents()));
        b.putInt("heldslot", player.getInventory().getHeldItemSlot());
        b.putString("world", player.getWorld().getName());
        b.putFloat("x", player.getLocation().getX());
        b.putFloat("y", player.getLocation().getY());
        b.putFloat("z", player.getLocation().getZ());
        b.putFloat("pitch", player.getLocation().getPitch());
        b.putFloat("yaw", player.getLocation().getYaw());
        b.putInt("gamemode", player.getGameMode().getValue());
        b.putBoolean("flying", player.isFlying());
        b.putInt("hunger", player.getFoodLevel());
        b.putFloat("health", player.getHealth());
        b.endMap(null, imap);
        ByteBuffer bb2 = b.finish();
        String key = "player-" + player.getUniqueId();
        WorldQLClient.pool.getResource().set(key, bb_to_str(bb2));

        ByteBuffer decodedInline = str_to_bb(bb_to_str(bb2));
        FlexBuffers.Map map = FlexBuffers.getRoot(decodedInline).asMap();
        System.out.println(map.size());
    }

    public static void setInventory(String flexBufferString, UUID uuid) {
        ByteBuffer r = str_to_bb(flexBufferString);
        // Has UUID, load inventory
        FlexBuffers.Map map = FlexBuffers.getRoot(r).asMap();
        System.out.println(map.size());

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        World world = Bukkit.getWorld(map.get("world").asString());
        if (world != null) {
            world = player.getWorld();
        }

        double x = map.get("x").asFloat();
        double y = map.get("y").asFloat();
        double z = map.get("z").asFloat();
        float pitch = (float) map.get("pitch").asFloat();
        float yaw = (float) map.get("yaw").asFloat();

        Location location = new Location(world, x, y, z, yaw, pitch);

        try {
            ItemStack[] inventory = ItemTools.deserializeItemStack(map.get("inventory").asBlob().data());
            player.getInventory().clear();
            player.getInventory().setContents(inventory);
            player.updateInventory();

            ItemStack[] echest = ItemTools.deserializeItemStack(map.get("echest").asBlob().data());
            player.getEnderChest().clear();
            player.getEnderChest().setContents(echest);

            player.getInventory().setHeldItemSlot(map.get("heldslot").asInt());
        } catch (IOException e) {
            e.printStackTrace();
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                player.setGameMode(GameMode.getByValue(map.get("gamemode").asInt()));
                player.setFlying(map.get("flying").asBoolean());
                player.teleport(location);

                player.setFoodLevel(map.get("hunger").asInt());
                player.setHealth(map.get("health").asFloat());
            }
        }.runTask(WorldQLClient.pluginInstance);
    }


}
