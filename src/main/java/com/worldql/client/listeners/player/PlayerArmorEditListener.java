package com.worldql.client.listeners.player;

import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.events.PlayerArmorEditEvent;
import com.worldql.client.serialization.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import zmq.ZMQ;

import java.nio.ByteBuffer;

public class PlayerArmorEditListener implements Listener {

    @EventHandler
    public void onArmorEdit(PlayerArmorEditEvent event) {
        FlexBuffersBuilder b = Codec.getFlexBuilder();
        int pmap = b.startMap();
        // Not to sure how we should send the ItemStack over using flat buffers?

        b.putBoolean("enchanted",!event.getNewPiece().getEnchantments().isEmpty());
        b.putString("material", event.getNewPiece().getType().name());
        b.putString("type", event.getArmorType().toString().toLowerCase());
        b.putString("username", event.getPlayer().getName());
        b.putString("uuid", event.getPlayer().getUniqueId().toString());
        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();

        Message message = new Message(
                Instruction.LocalMessage,
                WorldQLClient.worldQLClientId,
                event.getPlayer().getWorld().getName(),
                Replication.ExceptSelf,
                new Vec3D(event.getPlayer().getLocation()),
                null,
                null,
                "MinecraftPlayerEquipmentEdit",
                bb
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        ItemStack[] armor = player.getInventory().getArmorContents();
        if(e.getView().getType() != InventoryType.CRAFTING && e.getView().getTopInventory().getSize() != 5)return;
        if(e.getClick() == ClickType.DOUBLE_CLICK) {
            ItemStack cursor = e.getCursor();
            if(isAir(cursor))return;
            if(cursor.getAmount() == cursor.getType().getMaxStackSize())return;
            int i = -1;
            int amount = 0;
            ItemStack[] newArmor = new ItemStack[4];
            boolean changed = false;
            for(ItemStack item : armor) {
                i++;
                newArmor[i] = item;
                if(item==null)continue;
                if(!item.isSimilar(cursor))continue;
                PlayerArmorEditEvent event = new PlayerArmorEditEvent(player,36+i,item,(null), PlayerArmorEditEvent.ArmorType.fromSlot(36+i), PlayerArmorEditEvent.Cause.CURSOR_COLLECT);
                Bukkit.getServer().getPluginManager().callEvent(event);
                if(event.isCancelled()) {
                    newArmor[i] = item.clone();
                    amount += item.getAmount();
                    changed = true;
                    continue;
                }
                if(event.getNewPiece().equals(item))continue;
                changed = true;
            }
            int finalAmount = amount;
            if(changed)
                (new BukkitRunnable() {
                    @Override
                    public void run() {
                        cursor.setAmount(cursor.getAmount() - finalAmount);
                        for(ItemStack item : player.getInventory().getContents()) {
                            if(item!=null&&item.isSimilar(cursor)) {
                                while(cursor.getAmount() < cursor.getType().getMaxStackSize() && item.getAmount() > 0) {
                                    item.setAmount(item.getAmount() - 1);
                                    cursor.setAmount(cursor.getAmount() + 1);
                                }
                                break;
                            }
                        }
                        player.getInventory().setArmorContents(newArmor);
                        player.updateInventory();
                        player.setItemOnCursor(cursor);
                    }
                }).runTaskLater(WorldQLClient.getPluginInstance(), 0);
            return;
        }
        if(e.getSlot() >= 36 && e.getSlot() <= 39) {
            ItemStack oldItem = e.getCurrentItem();
            ItemStack newItem = e.getCursor();
            PlayerArmorEditEvent.Cause cause = PlayerArmorEditEvent.Cause.SET;
            if(isAir(newItem) || e.isShiftClick() ||
                    (newItem.isSimilar(oldItem) && (newItem.getAmount() + oldItem.getAmount()) < newItem.getType().getMaxStackSize())) cause = PlayerArmorEditEvent.Cause.TAKE;
            else if(!isAir(oldItem) && !isAir(newItem)) cause = PlayerArmorEditEvent.Cause.SWAP;
            PlayerArmorEditEvent.ArmorType aType = PlayerArmorEditEvent.ArmorType.fromSlot(e.getSlot());
            PlayerArmorEditEvent event = new PlayerArmorEditEvent(player, e.getSlot(), oldItem, newItem, aType, cause);
            if(PlayerArmorEditEvent.ArmorType.fromItem(newItem)==null)event.setAttemptNonArmor(true);
            if(PlayerArmorEditEvent.ArmorType.fromItem(newItem)!=null&&PlayerArmorEditEvent.ArmorType.fromSlot(e.getSlot())!=PlayerArmorEditEvent.ArmorType.fromItem(newItem))event.setAttemptedWrongSlot(true);
            Bukkit.getServer().getPluginManager().callEvent(event);
            if(event.isCancelled()) {
                e.setCancelled(true);
                return;
            }
            if(cause!= PlayerArmorEditEvent.Cause.TAKE && (!event.getNewPiece().equals(newItem) || ((event.isAttemptNonArmor()||event.isAttemptedWrongSlot())&&event.isForced()))) {
                ItemStack newI = event.getNewPiece();
                if(cause== PlayerArmorEditEvent.Cause.SET) {
                    ItemStack clone = isAir(newItem) ? new ItemStack(Material.AIR) : newItem.clone();
                    if(!isAir(clone)) clone.setAmount(clone.getAmount() - event.getNewPiece().getAmount());
                    e.setCursor(clone);
                }
                armor[aType.getId()] = newI;
                (new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.getInventory().setArmorContents(armor);
                        player.updateInventory();
                        player.setItemOnCursor(oldItem);
                    }
                }).runTaskLater(WorldQLClient.getPluginInstance(), 0);
            }
            ItemStack clone = isAir(oldItem) ? new ItemStack(Material.AIR) : oldItem.clone();
            if(!event.getOldPiece().equals(oldItem) && !isAir(event.getOldPiece())) {
                if(e.getClick() == ClickType.RIGHT||e.getClick()==ClickType.LEFT) {
                    ItemStack item = event.getOldPiece();
                    double amt = e.getClick()==ClickType.RIGHT ? item.getAmount()/2d : item.getAmount();
                    if(amt <= 0.5) amt = 1;
                    item.setAmount((int)amt);
                    e.setCursor(item);
                    if(cause == PlayerArmorEditEvent.Cause.TAKE) e.setCurrentItem(null);
                }else {
                    player.getInventory().addItem(event.getOldPiece());
                    if(!isAir(clone))
                        (new BukkitRunnable() {
                            @Override
                            public void run() {
                                player.getInventory().removeItem(clone);
                            }
                        }).runTaskLater(WorldQLClient.getPluginInstance(), 0);
                }
            }
            return;
        }
        if(!e.isShiftClick()) return;
        PlayerArmorEditEvent.ArmorType aType = PlayerArmorEditEvent.ArmorType.fromItem(e.getCurrentItem());
        if(aType == null) return;
        if(!isAir(armor[aType.getId()]))return;
        ItemStack oldItem = armor[aType.getId()];
        ItemStack newItem = e.getCurrentItem();
        PlayerArmorEditEvent event = new PlayerArmorEditEvent(player, e.getSlot(), oldItem, newItem, aType, PlayerArmorEditEvent.Cause.SET);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if(event.isCancelled()) {
            e.setCancelled(true);
            return;
        }
        armor[aType.getId()] = event.getNewPiece();
        (new BukkitRunnable() {
            @Override
            public void run() {
                if(!event.getOldPiece().equals(oldItem))
                    player.getInventory().setItem(e.getSlot(), event.getOldPiece());
                player.getInventory().setArmorContents(armor);
            }
        }).runTaskLater(WorldQLClient.getPluginInstance(), 0);
    }

    @EventHandler
    public void onBreak(PlayerItemBreakEvent e) {
        Player player = e.getPlayer();
        PlayerArmorEditEvent.ArmorType aType = PlayerArmorEditEvent.ArmorType.fromItem(e.getBrokenItem());
        if(aType == null) return;
        ItemStack[] armor = player.getInventory().getArmorContents();
        if(isAir(armor[aType.getId()])||!armor[aType.getId()].equals(e.getBrokenItem()))return;
        PlayerArmorEditEvent event = new PlayerArmorEditEvent(player, 36 + aType.getId(), e.getBrokenItem(), e.getBrokenItem(), aType, PlayerArmorEditEvent.Cause.BREAK);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if(event.isCancelled()) {
            ItemStack newI = e.getBrokenItem();
            newI.setDurability((short)(newI.getDurability()-1));
            armor[aType.getId()] = newI;
            player.getInventory().setArmorContents(armor);
            return;
        }
        if(!event.getNewPiece().isSimilar(e.getBrokenItem())) {
            armor[aType.getId()] = event.getNewPiece();
            player.getInventory().setArmorContents(armor);
        }

    }

    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        if(!event.hasItem())
            return;
        if (!event.getAction().name().contains("RIGHT"))
            return;

        PlayerArmorEditEvent.ArmorType aType = PlayerArmorEditEvent.ArmorType.fromItem(event.getItem());

        if(aType == null)
            return;
        if (event.getClickedBlock() != null)
            if (event.getClickedBlock().getType().isInteractable())
                return;

        Player player = event.getPlayer();
        ItemStack[] armor = player.getInventory().getArmorContents();

        if(!isAir(armor[aType.getId()]))return;
        ItemStack newItem = event.getItem();
        PlayerArmorEditEvent editEvent = new PlayerArmorEditEvent(player, player.getInventory().getHeldItemSlot(), (null), newItem, aType, PlayerArmorEditEvent.Cause.RIGHT_CLICK);
        Bukkit.getPluginManager().callEvent(editEvent);
        if(editEvent.isCancelled())
            event.setCancelled(true);
        if(!isAir(editEvent.getOldPiece()))
            player.getInventory().setItem(player.getInventory().getHeldItemSlot(), editEvent.getOldPiece());
        if(!editEvent.getNewPiece().equals(event.getItem())) {
            armor[aType.getId()] = editEvent.getNewPiece();
            player.getInventory().setArmorContents(armor);
        }
    }

    @EventHandler
    public void onDispense(BlockDispenseArmorEvent e) {
        if(!(e.getTargetEntity() instanceof Player))return;
        ItemStack item = e.getItem();
        PlayerArmorEditEvent.ArmorType aType = PlayerArmorEditEvent.ArmorType.fromItem(item);
        if(aType == null) return;
        Player player = (Player) e.getTargetEntity();
        ItemStack[] armor = player.getInventory().getArmorContents();
        if(!isAir(armor[aType.getId()]))return;
        ItemStack newItem = e.getItem();
        PlayerArmorEditEvent event = new PlayerArmorEditEvent(player, player.getInventory().getHeldItemSlot(), (null), newItem, aType, PlayerArmorEditEvent.Cause.DISPENSER);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if(event.isCancelled()) {
            e.setCancelled(true);
            return;
        }

        if(!isAir(event.getOldPiece())) {
            Dispenser dispenser = (Dispenser) e.getBlock().getState();
            dispenser.getInventory().addItem(event.getOldPiece());
        }
        if(!event.getNewPiece().equals(item)) {
            armor[aType.getId()] = event.getNewPiece();
            player.getInventory().setArmorContents(armor);
        }
    }


    private boolean isAir(Material mat) {
        return mat.name().endsWith("AIR") && !mat.name().endsWith("AIRS");
    }

    private boolean isAir(ItemStack item) {
        return item == null || isAir(item.getType());
    }
}
