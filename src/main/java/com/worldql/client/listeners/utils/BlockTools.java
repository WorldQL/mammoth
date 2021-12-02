package com.worldql.client.listeners.utils;

import com.google.flatbuffers.FlexBuffers;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.worldql.client.WorldQLClient;
import com.worldql.client.listeners.PlayerBreakBlockListener;
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
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.List;

public class BlockTools {
    public static void setRecords(List<Record> records, boolean isSelf) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Record record : records) {
                    Vec3D p = record.position();
                    Block b = Bukkit.getWorld(record.worldName())
                            .getBlockAt((int) p.x(), (int) p.y(), (int) p.z());
                    BlockData bd = Bukkit.createBlockData(record.data());
                    b.setBlockData(bd);

                    if (record.flex() != null) {
                        FlexBuffers.Map map = FlexBuffers.getRoot(record.flex()).asMap();

                        // TODO: Remove debug logging
                        WorldQLClient.getPluginInstance().getLogger().info("map = " + map.keys().toString());

                        // Handle drops
                        if (!map.get("drops").isNull() && isSelf && PlayerBreakBlockListener.pendingDrops.contains(record.uuid())) {
                            PlayerBreakBlockListener.pendingDrops.remove(record.uuid());

                            ItemStack[] drops = new ItemStack[0];
                            try {
                                drops = PlayerBreakBlockListener.deserializeItemStack(map.get("drops").asBlob().data());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            Location blockCenter = b.getLocation().add(0.5, 0.5, 0.5);
                            for (ItemStack item : drops) {
                                b.getWorld().dropItem(blockCenter, item);
                            }
                        }

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
                            }
                        }
                    }
                }
            }

        }.runTask(WorldQLClient.pluginInstance);
    }

    public static void createExplosion(Vec3D position, String worldName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                World w = Bukkit.getWorld(worldName);
                w.createExplosion(
                        position.x(), position.y(), position.z(),
                        4.0F, false, false);

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
}
