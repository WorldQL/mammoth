package com.worldql.client.listeners.utils;

import com.google.flatbuffers.FlexBuffers;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.worldql.client.WorldQLClient;
import com.worldql.client.listeners.world.PlayerBreakBlockListener;
import com.worldql.client.serialization.Codec;
import com.worldql.client.serialization.Record;
import com.worldql.client.serialization.Vec3D;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.MojangsonParser;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.block.entity.TileEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class BlockTools {
    public static Record serializeBlock(@NotNull Block block) {
        FlexBuffersBuilder b = Codec.getFlexBuilder();
        int pmap = b.startMap();

        Location location = block.getLocation();
        CraftWorld world = (CraftWorld) location.getWorld();
        TileEntity tile = world.getHandle().getBlockEntity(new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()), true);

        // Save NBT data
        if (tile != null) {
            NBTTagCompound nbt = tile.n();
            b.putBoolean("isTile", true);
            b.putString("nbt", nbt.toString());
        } else {
            b.putBoolean("isTile", false);
        }

        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();

        return new Record(
                UUID.nameUUIDFromBytes(location.toString().getBytes(StandardCharsets.UTF_8)),
                new Vec3D(location),
                world.getName(),
                block.getBlockData().getAsString(),
                bb
        );
    }

    public static Record airBlock(@NotNull Location location, @Nullable ItemStack[] drops) {
        FlexBuffersBuilder b = Codec.getFlexBuilder();
        int pmap = b.startMap();

        // Save drops
        if (drops != null) {
            b.putBlob("drops", ItemTools.serializeItemStack(drops));
        }

        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();

        return new Record(
                UUID.nameUUIDFromBytes(location.toString().getBytes(StandardCharsets.UTF_8)),
                new Vec3D(location),
                location.getWorld().getName(),
                "minecraft:air",
                bb
        );
    }

    public static void setRecords(List<Record> records, boolean isSelf) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Record record : records) {
                    setRecord(record, isSelf);
                }
            }

        }.runTask(WorldQLClient.pluginInstance);
    }

    private static void setRecord(@NotNull Record record, boolean isSelf) {
        Vec3D p = record.position();
        Block b = Bukkit.getWorld(record.worldName()).getBlockAt((int) p.x(), (int) p.y(), (int) p.z());

        BlockData bd = Bukkit.createBlockData(record.data());
        b.setBlockData(bd);

        if (record.flex() != null) {
            FlexBuffers.Map map = FlexBuffers.getRoot(record.flex()).asMap();

            // Handle drops
            if (!map.get("drops").isNull() && isSelf && PlayerBreakBlockListener.pendingDrops.contains(record.uuid())) {
                PlayerBreakBlockListener.pendingDrops.remove(record.uuid());

                ItemStack[] drops = new ItemStack[0];
                try {
                    drops = ItemTools.deserializeItemStack(map.get("drops").asBlob().data());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Location blockCenter = b.getLocation().add(0.5, 0.5, 0.5);
                for (ItemStack item : drops) {
                    b.getWorld().dropItem(blockCenter, item);
                }
            }

            // Handle NBT
            if (!map.get("isTile").isNull() && map.get("isTile").asBoolean()) {
                var nbtString = map.get("nbt").asString();
                NBTTagCompound copied = null;
                try {
                    copied = MojangsonParser.a(nbtString);
                } catch (CommandSyntaxException ex) {
                    ex.printStackTrace();
                }

                Location l = b.getLocation();
                CraftWorld cw = (CraftWorld) l.getWorld();
                TileEntity t = cw.getHandle().getBlockEntity(new BlockPosition(l.getBlockX(), l.getBlockY(), l.getBlockZ()), true);

                if (copied != null && t != null) {
                    t.a(copied);
                    b.getState().update();
                }
            }
        }
    }

    public static void createExplosion(Vec3D position, String worldName, float radius) {
        new BukkitRunnable() {
            @Override
            public void run() {
                World w = Bukkit.getWorld(worldName);
                w.createExplosion(
                        position.x(), position.y(), position.z(),
                        radius, false, false);

            }
        }.runTask(WorldQLClient.pluginInstance);
    }

    public static void createPrimedTNT(Vec3D position, String worldName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                World w = Bukkit.getWorld(worldName);
                Location tntLocation = new Location(w, position. x(), position.y(), position.z());
                TNTPrimed tnt = w.spawn(tntLocation, TNTPrimed.class);
                w.getBlockAt(tntLocation).setType(Material.AIR);

            }
        }.runTask(WorldQLClient.pluginInstance);
    }

    public static void createEndCrystal(Vec3D position, String worldName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                World w = Bukkit.getWorld(worldName);
                Location location = new Location(w, position. x(), position.y(), position.z());
                w.spawn(location, EnderCrystal.class);

            }
        }.runTask(WorldQLClient.pluginInstance);
    }
}
