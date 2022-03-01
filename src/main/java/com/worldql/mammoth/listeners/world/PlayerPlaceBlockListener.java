package com.worldql.mammoth.listeners.world;

import com.worldql.mammoth.Slices;
import com.worldql.mammoth.MammothPlugin;
import com.worldql.mammoth.events.PlayerHoldEvent;
import com.worldql.mammoth.listeners.utils.BlockTools;
import com.worldql.mammoth.worldql_serialization.Record;
import com.worldql.mammoth.worldql_serialization.*;
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
                        MammothPlugin.worldQLClientId,
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
                MammothPlugin.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
            }
        } else {
            Record placedBlock = BlockTools.serializeBlock(e.getBlockPlaced());
            Message message = new Message(
                    Instruction.RecordCreate,
                    MammothPlugin.worldQLClientId,
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
            MammothPlugin.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);

            // send a LocalMessage instruction with the same information so that clients can get an update on the chunk.
            Message localMessage = message.withInstruction(Instruction.LocalMessage);
            MammothPlugin.getPluginInstance().getPushSocket().send(localMessage.encode(), ZMQ.ZMQ_DONTWAIT);
        }

        // Update hand visual if they ran out of blocks in their hand.
        if (e.getPlayer().getInventory().getItemInMainHand().getAmount() - 1 <= 0) {
            Bukkit.getPluginManager().callEvent(new PlayerHoldEvent(e.getPlayer(), new ItemStack(Material.AIR), PlayerHoldEvent.HandType.MAINHAND));
        }
    }
}
