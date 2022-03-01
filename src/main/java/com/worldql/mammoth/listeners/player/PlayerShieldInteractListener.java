package com.worldql.mammoth.listeners.player;

import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.mammoth.MammothPlugin;
import com.worldql.mammoth.worldql_serialization.*;
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
        FlexBuffersBuilder b = Codec.getFlexBuilder();
        int pmap = b.startMap();

        b.putBoolean("blocking", blocking);
        b.putBoolean("offhand", offhand);
        b.putString("username",player.getName());
        b.putString("uuid", player.getUniqueId().toString());
        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();

        Message message = new Message(
                Instruction.LocalMessage,
                MammothPlugin.worldQLClientId,
                player.getWorld().getName(),
                Replication.ExceptSelf,
                new Vec3D(player.getLocation()),
                null,
                null,
                "MinecraftPlayerShieldUse",
                bb
        );

        MammothPlugin.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
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
                }.runTask(MammothPlugin.getPluginInstance());
                if (stopped)
                    cancel();
            }
        }.runTaskTimerAsynchronously(MammothPlugin.getPluginInstance(), 6, 0);
    }
}
