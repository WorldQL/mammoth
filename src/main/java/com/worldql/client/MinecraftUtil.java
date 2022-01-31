package com.worldql.client;

import com.worldql.client.listeners.utils.BlockTools;
import com.worldql.client.worldql_serialization.*;
import com.worldql.client.worldql_serialization.Record;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

public class MinecraftUtil {
    public static String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeInt(items.length);
            for (int i = 0; i < items.length; i++) {
                dataOutput.writeObject(items[i]);
            }
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    public static ItemStack[] itemStackArrayFromBase64(String data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
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

    public static void detectAndBroadcastChangedNeighborBlocks(Block placedBlock) {
        final String[] relatives = {
                placedBlock.getRelative(1, 0, 0).getBlockData().getAsString(),
                placedBlock.getRelative(-1, 0, 0).getBlockData().getAsString(),
                placedBlock.getRelative(0, 1, 0).getBlockData().getAsString(),
                placedBlock.getRelative(0, -1, 0).getBlockData().getAsString(),
                placedBlock.getRelative(0, 0, 1).getBlockData().getAsString(),
                placedBlock.getRelative(0, 0, -1).getBlockData().getAsString(),
        };
        final Location[] relativeLocations = {
                placedBlock.getRelative(1, 0, 0).getLocation(),
                placedBlock.getRelative(-1, 0, 0).getLocation(),
                placedBlock.getRelative(0, 1, 0).getLocation(),
                placedBlock.getRelative(0, -1, 0).getLocation(),
                placedBlock.getRelative(0, 0, 1).getLocation(),
                placedBlock.getRelative(0, 0, -1).getLocation(),
        };
        Bukkit.getScheduler().runTaskLater(WorldQLClient.getPluginInstance(), () -> {
            String[] newRelatives = {
                    placedBlock.getRelative(1, 0, 0).getBlockData().getAsString(),
                    placedBlock.getRelative(-1, 0, 0).getBlockData().getAsString(),
                    placedBlock.getRelative(0, 1, 0).getBlockData().getAsString(),
                    placedBlock.getRelative(0, -1, 0).getBlockData().getAsString(),
                    placedBlock.getRelative(0, 0, 1).getBlockData().getAsString(),
                    placedBlock.getRelative(0, 0, -1).getBlockData().getAsString(),
            };
            for (int i = 0; i < 6; i++) {
                // seems like the 1.17.1 update made this entire "wait 3 ticks" trick worthless. door adjacent blocks
                // update before the event handler is called.
                if (!relatives[i].equals(newRelatives[i])
                ) {
                    Record sideEffectBlock = BlockTools.serializeBlock(relativeLocations[i].getBlock());
                    Message message = new Message(
                            Instruction.RecordCreate,
                            WorldQLClient.worldQLClientId,
                            relativeLocations[i].getWorld().getName(),
                            Replication.ExceptSelf,
                            // This field isn't really used since the Record also contains the position
                            // of the changed block(s).
                            new Vec3D(relativeLocations[i]),
                            List.of(sideEffectBlock),
                            null,
                            "MinecraftBlockUpdate",
                            null
                    );
                }
            }
        }, 2L);
    }


}
