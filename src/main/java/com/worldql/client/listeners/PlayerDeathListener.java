package com.worldql.client.listeners;

import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.serialization.Codec;
import com.worldql.client.serialization.Instruction;
import com.worldql.client.serialization.Message;
import com.worldql.client.serialization.Replication;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import zmq.ZMQ;

import java.nio.ByteBuffer;
import java.util.UUID;

public class PlayerDeathListener implements Listener {
    public static final ItemStack[] EMPTY_DROPS = new ItemStack[0];

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        String killerUuid = null;
        if (e.getEntity().getKiller() != null) {
            killerUuid = e.getEntity().getKiller().getUniqueId().toString();
        }

        ItemStack[] drops = EMPTY_DROPS;
        if (killerUuid != null) {
            drops = e.getDrops().toArray(new ItemStack[0]);
        }

        // Send death event to other servers
        FlexBuffersBuilder b = Codec.getFlexBuilder();
        int pmap = b.startMap();
        b.putString("uuid", e.getEntity().getUniqueId().toString());
        if (killerUuid != null) b.putString("killer", killerUuid);
        b.putString("message", e.getDeathMessage());
        b.putBlob("drops", PlayerBreakBlockListener.serializeItemStack(drops));
        b.putInt("xp", e.getDroppedExp());
        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();

        Message message = new Message(
                Instruction.GlobalMessage,
                WorldQLClient.worldQLClientId,
                "@global",
                Replication.IncludingSelf,
                null,
                null,
                null,
                "MinecraftPlayerDeath",
                bb
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);

        // Stop drops from dropping if killed by a player
        if (killerUuid != null) {
            e.getDrops().clear();
        }
    }
}
