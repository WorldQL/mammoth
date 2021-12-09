package com.worldql.client.listeners.player;

import WorldQLFB_OLD.StandardEvents.Update;
import WorldQLFB_OLD.StandardEvents.Vec3;
import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.Messages.Instruction;
import com.worldql.client.Messages.Message;
import com.worldql.client.Messages.Vec3d;
import com.worldql.client.WorldQLClient;
import com.worldql.client.listeners.utils.OutgoingMinecraftPlayerSingleAction;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.block.entity.TileEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import zmq.ZMQ;

import java.nio.ByteBuffer;
import java.util.List;

public class PlayerInteractEventListener implements Listener {
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        /* Some code for getting the NBT data of an object.
        if (e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            Location l = e.getClickedBlock().getLocation();
            CraftWorld cw = (CraftWorld) e.getClickedBlock().getLocation().getWorld();
            TileEntity t = cw.getHandle().getTileEntity(new BlockPosition(l.getBlockX(), l.getBlockY(), l.getBlockZ()));
            if (t == null) {
                e.getPlayer().sendMessage("Not a tile entity.");
            } else {
                e.getPlayer().sendMessage("This is a tile entity!");

                NBTTagCompound nbt = new NBTTagCompound();
                t.save(nbt);
                System.out.println(nbt.toString());
            }
            return;
        }
         */

        // This is apparently the most elegant way to do this...
        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                && e.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.END_CRYSTAL)
                && e.getClickedBlock().getType().equals(Material.OBSIDIAN)) {
            // Thanks to https://www.spigotmc.org/threads/trying-to-detect-when-end-crystals-spawn-by-player.375673/
            (new BukkitRunnable() {
                @Override
                public void run() {
                    // We could also do this with getTileEntity but the Bukkit API is preferred over nms.
                    List<Entity> entities = e.getPlayer().getNearbyEntities(4, 4, 4);
                    for (Entity entity : entities) {
                        if (EntityType.ENDER_CRYSTAL == entity.getType()) {
                            EnderCrystal crystal = (EnderCrystal) entity;
                            Block belowCrystal = crystal.getLocation().getBlock().getRelative(BlockFace.DOWN);
                            if (e.getClickedBlock().equals(belowCrystal)) {
                                System.out.println("WE PLACED AN END CRYSTAL");
                                OutgoingMinecraftPlayerSingleAction.sendPlaceEndCrystalPacket(
                                        e.getPlayer().getWorld(),
                                        crystal.getLocation()
                                );
                                return;
                            }
                        }
                    }
                }
            }).runTask(WorldQLClient.getPluginInstance());

        }

        Location playerLocation = e.getPlayer().getLocation();
        String action = "punch";
        OutgoingMinecraftPlayerSingleAction.sendPacket(playerLocation, e.getPlayer(), action);
    }
}
