package com.worldql.client.protocols;

import com.google.flatbuffers.FlexBuffers;
import com.mojang.datafixers.util.Pair;
import com.worldql.client.serialization.Message;
import net.minecraft.network.protocol.game.PacketPlayOutEntityEquipment;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.List;

public class MinecraftPlayerEquipmentEdit {

    public static void process(Message state, EntityPlayer entity) {
        FlexBuffers.Map playerMessageMap = FlexBuffers.getRoot(state.flex()).asMap();
        List<Pair<EnumItemSlot, ItemStack>> equipment = new ArrayList<>();
        EnumItemSlot slot = EnumItemSlot.a(playerMessageMap.get("type").asString());

        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(
                Material.valueOf(playerMessageMap.get("material").asString()));

        // adds enchantment visual
        if (playerMessageMap.get("enchanted").asBoolean())
            item.addEnchantment(Enchantment.DURABILITY,1);

        equipment.add(new Pair<>(slot, CraftItemStack.asNMSCopy(item)));

        ProtocolManager.sendGenericPacket(new PacketPlayOutEntityEquipment(entity.ae(), equipment));
    }
}
