package com.worldql.mammoth;

import com.worldql.mammoth.worldql_serialization.*;
import com.worldql.mammoth.worldql_serialization.Record;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Door;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;
import zmq.ZMQ;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

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

    public static void setBed(Block head, BlockFace facing, Material material) {
        for (Bed.Part part : Bed.Part.values()) {
            Bed bedData = (Bed) Bukkit.createBlockData(material, (data) -> {
                ((Bed) data).setPart(part);
                ((Bed) data).setFacing(facing);
            });
            head.setBlockData(bedData);
            head = head.getRelative(facing.getOppositeFace());
        }
    }

    public static void sendAirBlock(Location l) {
        Record airBlock = new Record(
                UUID.nameUUIDFromBytes(l.toString().getBytes(StandardCharsets.UTF_8)),
                new Vec3D(l),
                l.getWorld().getName(),
                "minecraft:air",
                null
        );
        Message message = new Message(
                Instruction.GlobalMessage,
                MammothPlugin.worldQLClientId,
                "@global",
                Replication.IncludingSelf,
                // This field isn't really used since the Record also contains the position
                // of the changed block(s).
                new Vec3D(l),
                List.of(airBlock),
                null,
                "MinecraftBlockUpdate",
                null
        );
        MammothPlugin.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
    }
    public static void breakConnectedBlock(Block b) {
        BlockData bd = b.getBlockData();
        if (bd instanceof Door door) {
            Location target;
            if (door.getHalf().equals(Bisected.Half.BOTTOM)) {
                target = b.getRelative(BlockFace.UP).getLocation();
            } else {
                target = b.getRelative(BlockFace.DOWN).getLocation();
            }
            sendAirBlock(target);
            return;
        }
        if (bd instanceof Bed) {
            Bed bed = (Bed) bd;
            Location target;
            target = b.getRelative(bed.getFacing().getOppositeFace()).getLocation();
            sendAirBlock(target);
            return;
        }
    }
}
