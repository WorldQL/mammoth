package com.worldql.client.listeners;

import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.events.PlayerHoldEvent;
import com.worldql.client.serialization.Codec;
import com.worldql.client.serialization.Instruction;
import com.worldql.client.serialization.Message;
import com.worldql.client.serialization.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import zmq.ZMQ;

import java.nio.ByteBuffer;

public class PlayerHeldItemListener implements Listener {

    @EventHandler
    public void onItemHeld(PlayerHoldEvent event) {
        FlexBuffersBuilder b = Codec.getFlexBuilder();
        int pmap = b.startMap();
        // Not to sure how we should send the ItemStack over using flat buffers?

        b.putBoolean("enchanted", !event.getPiece().getEnchantments().isEmpty());
        b.putString("material", event.getPiece().getType().name());
        b.putString("type", event.getType().toString().toLowerCase());
        b.putString("username", event.getPlayer().getName());
        b.putString("uuid", event.getPlayer().getUniqueId().toString());
        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();

        Message message = new Message(
                Instruction.LocalMessage,
                WorldQLClient.worldQLClientId,
                event.getPlayer().getWorld().getName(),
                new Vec3D(event.getPlayer().getLocation()),
                null,
                null,
                "MinecraftPlayerEquipmentEdit",
                bb
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());

        Bukkit.getPluginManager().callEvent(new PlayerHoldEvent(player, item, PlayerHoldEvent.HandType.MAINHAND));
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (event.getPlayer().getInventory().getItemInMainHand().getAmount() - 1 >= 0)
            return;
        Player player = event.getPlayer();
        ItemStack item = new ItemStack(Material.AIR);

        Bukkit.getPluginManager().callEvent(new PlayerHoldEvent(player, item, PlayerHoldEvent.HandType.MAINHAND));

    }


    @EventHandler
    public void onInventoryInteract(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = player.getInventory().getHeldItemSlot();

        if (event.getSlot() == slot) {
            Bukkit.getPluginManager().callEvent(new PlayerHoldEvent(player, event.getCursor(), PlayerHoldEvent.HandType.MAINHAND));
            return;
        }
        if (event.getSlot() == 40)
            Bukkit.getPluginManager().callEvent(new PlayerHoldEvent(player, event.getCursor(), PlayerHoldEvent.HandType.OFFHAND));
    }

    @EventHandler
    public void onInventoryShiftClick(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (event.getInventorySlots().contains(player.getInventory().getHeldItemSlot()))
            Bukkit.getPluginManager().callEvent(new PlayerHoldEvent(player, event.getCursor(), PlayerHoldEvent.HandType.MAINHAND));
    }

    @EventHandler
    public void onItemSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        Bukkit.getPluginManager().callEvent(new PlayerHoldEvent(player, event.getMainHandItem(), PlayerHoldEvent.HandType.MAINHAND));
        Bukkit.getPluginManager().callEvent(new PlayerHoldEvent(player, event.getOffHandItem(), PlayerHoldEvent.HandType.OFFHAND));
    }


    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!player.getInventory().getItemInMainHand().getType().equals(Material.AIR))
                return;
            ItemStack stack = event.getItem().getItemStack();
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.getInventory().getItemInMainHand().equals(stack)) {
                        Bukkit.getScheduler().runTask(WorldQLClient.getPluginInstance(),
                                () -> Bukkit.getPluginManager().callEvent(new PlayerHoldEvent(player, stack, PlayerHoldEvent.HandType.MAINHAND)));
                    }
                }
            }.runTaskAsynchronously(WorldQLClient.getPluginInstance());
        }
    }
}
