package com.worldql.client.listeners;

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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import zmq.ZMQ;

import java.nio.ByteBuffer;

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


        if (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
            return;
        Location playerLocation = e.getPlayer().getLocation();
        String action = "punch";
        OutgoingMinecraftPlayerSingleAction.sendPacket(playerLocation, e.getPlayer(), action);
    }
}
