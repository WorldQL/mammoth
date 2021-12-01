package com.worldql.client.listeners;

import WorldQLFB_OLD.StandardEvents.Vec3;
import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.serialization.Record;
import com.worldql.client.serialization.*;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import zmq.ZMQ;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PlayerBreakBlockListener implements Listener {
    public static final ItemStack[] NO_DROPS = new ItemStack[0];
    public static final Set<UUID> pendingDrops = Collections.synchronizedSet(new HashSet<>());

    @EventHandler
    public void onPlayerBreakBlockEvent(BlockBreakEvent e) {
        ItemStack[] drops;
        if (e.isDropItems() && !e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
            drops = e.getBlock().getDrops().toArray(new ItemStack[0]);

            e.setDropItems(false);
        } else {
            drops = NO_DROPS;
        }

        UUID blockUuid = UUID.nameUUIDFromBytes(e.getBlock().getLocation().toString().getBytes(StandardCharsets.UTF_8));
        pendingDrops.add(blockUuid);

        Record airBlock = new Record(
                blockUuid,
                new Vec3D(e.getBlock().getLocation()),
                e.getBlock().getWorld().getName(),
                "minecraft:air",
                ByteBuffer.wrap(serializeItemStack(drops))
        );

        Message message = new Message(
                Instruction.LocalMessage,
                WorldQLClient.worldQLClientId,
                e.getPlayer().getWorld().getName(),
                Replication.IncludingSelf,
                // This field isn't really used since the Record also contains the position
                // of the changed block(s).
                new Vec3D(e.getBlock().getLocation()),
                List.of(airBlock),
                null,
                "MinecraftBlockUpdate",
                null
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);

        // Don't pass drops flex to DB
        Message recordMessage = message.withInstruction(Instruction.RecordCreate)
                .withParameter(null)
                .withRecords(List.of(airBlock.withFlex(null)));

        WorldQLClient.getPluginInstance().getPushSocket().send(recordMessage.encode(), ZMQ.ZMQ_DONTWAIT);
    }

    public static byte[] serializeItemStack(ItemStack[] items) throws IllegalStateException {
        // TODO: Make this serialization more efficient
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeInt(items.length);

            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }

            dataOutput.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    public static ItemStack[] deserializeItemStack(ByteBuffer buf) throws IOException {
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

            ItemStack[] items = new ItemStack[dataInput.readInt()];
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            dataInput.close();
            return items;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }

    @Deprecated
    public static int createRoundedVec3(FlatBufferBuilder builder, double x, double y, double z) {
        return Vec3.createVec3(builder, Math.round(x), Math.round(y), Math.round(z));
    }
}
