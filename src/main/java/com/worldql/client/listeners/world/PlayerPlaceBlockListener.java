package com.worldql.client.listeners.world;

import com.worldql.client.MinecraftUtil;
import com.worldql.client.Slices;
import com.worldql.client.WorldQLClient;
import com.worldql.client.events.PlayerHoldEvent;
import com.worldql.client.listeners.utils.BlockTools;
import com.worldql.client.worldql_serialization.Record;
import com.worldql.client.worldql_serialization.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import zmq.ZMQ;

import java.util.List;

public class PlayerPlaceBlockListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPlaceBlockEvent(BlockPlaceEvent e) {
        if (e.isCancelled() || e.getBlockPlaced().getType().equals(Material.FIRE)) {
            return;
        }

        if (Slices.enabled) {
            /*
            if (Slices.isDMZ(e.getBlockPlaced().getLocation()) && e.getBlockPlaced().getState() instanceof Container) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You cannot place containers here.");
                return;
            }
             */
            if (Slices.isDMZ(e.getBlockPlaced().getLocation())) {
                // TODO: Handle compound blocks (beds, doors) and joined blocks (fences, glass panes)
                Record placedBlock = BlockTools.serializeBlock(e.getBlockPlaced());
                Message message = new Message(
                        Instruction.GlobalMessage,
                        WorldQLClient.worldQLClientId,
                        "@global",
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
                MinecraftUtil.detectAndBroadcastChangedNeighborBlocks(e.getBlockPlaced());
            }
        } else {
            Record placedBlock = BlockTools.serializeBlock(e.getBlockPlaced());
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
        }

        // Update hand visual if they ran out of blocks in their hand.
        if (e.getPlayer().getInventory().getItemInMainHand().getAmount() - 1 <= 0) {
            Bukkit.getPluginManager().callEvent(new PlayerHoldEvent(e.getPlayer(), new ItemStack(Material.AIR), PlayerHoldEvent.HandType.MAINHAND));
        }

    }

}
