package com.worldql.client.listeners;

import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.events.PlayerHoldEvent;
import com.worldql.client.serialization.Record;
import com.worldql.client.serialization.*;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.block.entity.TileEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import zmq.ZMQ;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class PlayerPlaceBlockListener implements Listener {

    @EventHandler
    public void onPlayerPlaceBlockEvent(BlockPlaceEvent e) {
        FlexBuffersBuilder b = Codec.getFlexBuilder();
        int pmap = b.startMap();


        Location l = e.getBlockPlaced().getLocation();
        CraftWorld cw = (CraftWorld) e.getBlockPlaced().getLocation().getWorld();
        TileEntity t = cw.getHandle().getBlockEntity(new BlockPosition(l.getBlockX(), l.getBlockY(), l.getBlockZ()), true);

        // Save NBT data to flexbuffer
        if (t != null) {
            NBTTagCompound nbt = t.n();
            b.putBoolean("isTile", true);
            b.putString("nbt", nbt.toString());
        } else {
            b.putBoolean("isTile", false);
        }

        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();

        Record placedBlock = new Record(
                UUID.nameUUIDFromBytes(e.getBlockPlaced().getLocation().toString().getBytes(StandardCharsets.UTF_8)),
                new Vec3D(e.getBlockPlaced().getLocation()),
                e.getBlockPlaced().getWorld().getName(),
                e.getBlockPlaced().getBlockData().getAsString(),
                bb
        );
        // TODO: Handle compound blocks (beds, doors) and joined blocks (fences, glass panes)

        Message message = new Message(
                Instruction.RecordCreate,
                WorldQLClient.worldQLClientId,
                e.getPlayer().getWorld().getName(),
                Replication.ExceptSelf,
                // This field isn't really used since the Record also contains the position
                // of the changed block(s).
                new Vec3D(e.getBlockPlaced().getLocation()),
                List.of(placedBlock),
                null,
                "MinecraftBlockUpdate",
                null
        );
        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);

        // send a LocalMessage instruction with the same information so that clients can get an update on the chunk.
        Message localMessage = message.withInstruction(Instruction.LocalMessage);
        WorldQLClient.getPluginInstance().getPushSocket().send(localMessage.encode(), ZMQ.ZMQ_DONTWAIT);



        // Update hand visual if they ran out of blocks in their hand.
        if (e.getPlayer().getInventory().getItemInMainHand().getAmount() -1 <= 0)
            Bukkit.getPluginManager().callEvent(new PlayerHoldEvent(e.getPlayer(), new ItemStack(Material.AIR), PlayerHoldEvent.HandType.MAINHAND));
    }
}
