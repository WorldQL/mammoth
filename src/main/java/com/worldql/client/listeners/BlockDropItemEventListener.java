package com.worldql.client.listeners;

import com.worldql.client.MinecraftUtil;
import com.worldql.client.WorldQLClient;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;

public class BlockDropItemEventListener implements Listener {

    @EventHandler
    public void onBlockDrop(BlockDropItemEvent event) throws IOException {
        BlockState blockState = event.getBlockState();
        if (blockState instanceof Container) {
            ItemStack[] items = ((Container) blockState).getInventory().getContents();
            for (ItemStack item : items) {
                WorldQLClient.getPluginInstance().getLogger().info(item.toString());
            }

            String itemsToBase64 = MinecraftUtil.itemStackArrayToBase64(items);
            ItemStack[] itemsFromBase64 = MinecraftUtil.itemStackArrayFromBase64(itemsToBase64);
            for (ItemStack item : itemsFromBase64) {
                WorldQLClient.getPluginInstance().getLogger().info(item.toString());
            }
            System.out.println(itemsToBase64);
        }
        event.setCancelled(true);
    }
}
