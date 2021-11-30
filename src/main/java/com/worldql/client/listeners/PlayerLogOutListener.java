package com.worldql.client.listeners;

import com.google.flatbuffers.FlexBuffers;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.listeners.utils.OutgoingMinecraftPlayerSingleAction;
import com.worldql.client.protocols.Protocol;
import com.worldql.client.protocols.ProtocolManager;
import com.worldql.client.serialization.*;
import com.worldql.client.serialization.Record;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.libs.org.apache.commons.io.output.ByteArrayOutputStream;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import zmq.ZMQ;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class PlayerLogOutListener implements Listener {
    public static final Set<UUID> pendingInventories = Collections.synchronizedSet(new HashSet<>());

    @EventHandler
    public void onPlayerLogOut(PlayerQuitEvent e) {
        pendingInventories.remove(e.getPlayer().getUniqueId());

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
        saveInventory(player);
    }

    @EventHandler
    public void onPlayerLogIn(PlayerJoinEvent e) {
        pendingInventories.add(e.getPlayer().getUniqueId());
        Message recordMessage = new Message(
                Instruction.RecordRead,
                WorldQLClient.worldQLClientId,
                "inventory",
                new Vec3D(0, 0, 0)
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(recordMessage.encode(), ZMQ.ZMQ_DONTWAIT);
    }

    public static void saveInventory(Player player) {
        FlexBuffersBuilder b = Codec.getFlexBuilder();
        int imap = b.startMap();

        b.putBlob("inventory", PlayerBreakBlockListener.serializeItemStack(player.getInventory().getContents()));
        b.putBlob("echest", PlayerBreakBlockListener.serializeItemStack(player.getEnderChest().getContents()));
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

        Vec3D zero = new Vec3D(0, 0, 0);
        Record playerRecord = new Record(player.getUniqueId(), zero, "inventory", null, bb2);
        Message recordsMessage = new Message(
                Instruction.RecordCreate,
                WorldQLClient.worldQLClientId,
                "inventory",
                Replication.ExceptSelf,
                zero,
                List.of(playerRecord),
                null,
                null,
                null
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(recordsMessage.encode(), ZMQ.ZMQ_DONTWAIT);
    }

    public static void setInventories(List<Record> records) {
        Collections.reverse(records);

        List<Record> toDelete = new ArrayList<>();

        for (Record record : records) {
            if (!pendingInventories.contains(record.uuid())) {
                continue;
            }

            // Has UUID, load inventory
            FlexBuffers.Map map = FlexBuffers.getRoot(record.flex()).asMap();

            // Remove from pending and database
            pendingInventories.remove(record.uuid());
            toDelete.add(record.withFlex(null));

            Player player = Bukkit.getPlayer(record.uuid());
            if (player == null) continue;

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
                ItemStack[] inventory = PlayerBreakBlockListener.deserializeItemStack(map.get("inventory").asBlob().data());
                player.getInventory().clear();
                player.getInventory().setContents(inventory);
                player.updateInventory();

                ItemStack[] echest = PlayerBreakBlockListener.deserializeItemStack(map.get("echest").asBlob().data());
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

        Message deleteRecordsMsg = new Message(
                Instruction.RecordDelete,
                WorldQLClient.worldQLClientId,
                "inventory",
                Replication.ExceptSelf,
                new Vec3D(0, 0, 0),
                toDelete,
                null,
                null,
                null
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(deleteRecordsMsg.encode(), ZMQ.ZMQ_DONTWAIT);
    }
}
