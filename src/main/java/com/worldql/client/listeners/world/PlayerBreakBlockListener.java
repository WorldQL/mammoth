package com.worldql.client.listeners.world;

import com.worldql.client.WorldQLClient;
import com.worldql.client.listeners.utils.BlockTools;
import com.worldql.client.serialization.Record;
import com.worldql.client.serialization.*;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import zmq.ZMQ;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class PlayerBreakBlockListener implements Listener {
    public static final ItemStack[] NO_DROPS = new ItemStack[0];
    public static final Set<UUID> pendingDrops = Collections.synchronizedSet(new HashSet<>());

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBreakBlockEvent(BlockBreakEvent e) {
        if (e.isCancelled()) {
            return;
        }
        ItemStack[] drops;
        if (e.isDropItems() && !e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
            drops = e.getBlock().getDrops().toArray(new ItemStack[0]);
            e.setDropItems(false);
        } else {
            drops = NO_DROPS;
        }

        UUID blockUuid = UUID.nameUUIDFromBytes(e.getBlock().getLocation().toString().getBytes(StandardCharsets.UTF_8));
        pendingDrops.add(blockUuid);

        Record airBlock = BlockTools.airBlock(e.getBlock().getLocation(), drops);
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

}
