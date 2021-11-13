package com.worldql.client.listeners;

import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.Messages.Instruction;
import com.worldql.client.Messages.Message;
import com.worldql.client.Messages.Vec3d;
import com.worldql.client.WorldQLClient;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import zmq.ZMQ;

import java.nio.ByteBuffer;

public class PlayerShieldInteractListener implements Listener {

    private void sendPacket(Player player, boolean blocking, boolean offhand) {
        FlexBuffersBuilder b = new FlexBuffersBuilder();
        int pmap = b.startMap();

        b.putBoolean("blocking", blocking);
        b.putBoolean("offhand", offhand);
        b.putString("username",player.getName());
        b.putString("uuid", player.getUniqueId().toString());
        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();

        FlatBufferBuilder builder = new FlatBufferBuilder(1024);

        int sender_uuid = builder.createString(WorldQLClient.worldQLClientId);
        int worldName = builder.createString(player.getWorld().getName());
        int command = builder.createString("MinecraftPlayerShieldUse");
        int flex = builder.createByteVector(bb);

        Message.startMessage(builder);
        Message.addInstruction(builder, Instruction.LocalMessage);
        Message.addWorldName(builder, worldName);
        Message.addParameter(builder, command);
        Message.addSenderUuid(builder, sender_uuid);
        Location loc = player.getLocation();
        Message.addPosition(builder, Vec3d.createVec3d(builder, (float) loc.getX(), (float) loc.getY(), (float) loc.getZ()));
        Message.addFlex(builder, flex);

        int message = Message.endMessage(builder);
        builder.finish(message);

        byte[] buf = builder.sizedByteArray();
        WorldQLClient.getPluginInstance().getPushSocket().send(buf, ZMQ.ZMQ_DONTWAIT);
    }

    @EventHandler
    public void onShieldUse(PlayerInteractEvent event) {
        if (!event.hasItem())
            return;
        if (!event.getItem().getType().equals(Material.SHIELD))
            return;
        if (event.getClickedBlock() != null && event.getClickedBlock().getType().isInteractable())
            return;
        Player player = event.getPlayer();
        boolean offhand = player.getInventory().getItemInOffHand().getType().equals(Material.SHIELD);
        sendPacket(player, true, offhand);

        // kind of junky, but this is how we currently detect when they are no longer using a shield
        // It runs async then sync to do the isBlocking() api check
        new BukkitRunnable() {
            boolean stopped = false;
            @Override
            public void run() {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isBlocking()) {
                            sendPacket(player, false, offhand);
                            stopped = true;
                            cancel();
                        }
                    }
                }.runTask(WorldQLClient.getPluginInstance());
                if (stopped)
                    cancel();
            }
        }.runTaskTimerAsynchronously(WorldQLClient.getPluginInstance(), 6, 0);
    }
}
