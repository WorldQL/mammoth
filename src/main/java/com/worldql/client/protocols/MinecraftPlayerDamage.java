package com.worldql.client.protocols;

import com.google.flatbuffers.FlexBuffers;
import com.worldql.client.WorldQLClient;
import com.worldql.client.ghost.ExpiringEntityPlayer;
import com.worldql.client.serialization.Message;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

public class MinecraftPlayerDamage {

    private static final Random random = new Random();
    //                                           receiver          attacker
    public static void process(Message state, Player player, ExpiringEntityPlayer e) {
        FlexBuffers.Map playerMessageMap = FlexBuffers.getRoot(state.flex()).asMap();
        boolean sprinting = playerMessageMap.get("sprinting").asBoolean();
        float knockbacklvl = (float) playerMessageMap.get("knockbacklvl").asFloat();
        float damage = (float) playerMessageMap.get("damage").asFloat();

        double resistance = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).getValue();

        // Calculate and issue knockback
        if (random.nextDouble() >= resistance){
            double dist = sprinting? 1.5:1.0;
            dist += random.nextDouble()*0.4-0.2;
            dist += 1.55*(int)knockbacklvl;

            double mag = (dist + 1.5)/5.0;
            Location location = e.grab().getBukkitEntity().getLocation();

            // TODO Fix this, has an infinite fly glitch.
            if(player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR)
                location.setPitch(80);
            else
                location.setPitch((knockbacklvl < 1)? -40: -26);

            Vector velocity = setMag((location.getDirection()), mag);
            player.setVelocity(velocity);
        }
        // Calculate and issue damage
        double points = player.getAttribute(Attribute.GENERIC_ARMOR).getValue();
        double toughness = player.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).getValue();
        PotionEffect effect = player.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
        int resist = effect == null ? 0 : effect.getAmplifier();
        int epf = getEPF(player.getInventory());

        // need sync to run this, even if we do p.setHealth(health - damage); we still need it to be sync
        new BukkitRunnable() {
            @Override
            public void run() {
                player.damage(calculateDamageApplied(damage, points, toughness, resist, epf), e.grab().getBukkitEntity());
            }
        }.runTask(WorldQLClient.getPluginInstance());
    }



    private static Vector setMag(Vector v, double mag){
        double denominator = Math.sqrt(v.getX()*v.getX() + v.getY()*v.getY() + v.getZ()*v.getZ());

        return (denominator != 0 ) ? v.multiply(mag/denominator) : v;
    }



    private static double calculateDamageApplied(double damage, double points, double toughness, int resistance, int epf) {
        double withArmorAndToughness = damage * (1 - Math.min(20, Math.max(points / 5, points - damage / (2 + toughness / 4))) / 25);
        double withResistance = withArmorAndToughness * (1 - (resistance * 0.2));
        return withResistance * (1 - (Math.min(20.0, epf) / 25));
    }



    private static int getEPF(PlayerInventory inv) {
        ItemStack helm = inv.getHelmet();
        ItemStack chest = inv.getChestplate();
        ItemStack legs = inv.getLeggings();
        ItemStack boot = inv.getBoots();

        return (helm != null ? helm.getEnchantmentLevel(Enchantment.DAMAGE_ALL) : 0) +
                (chest != null ? chest.getEnchantmentLevel(Enchantment.DAMAGE_ALL) : 0) +
                (legs != null ? legs.getEnchantmentLevel(Enchantment.DAMAGE_ALL) : 0) +
                (boot != null ? boot.getEnchantmentLevel(Enchantment.DAMAGE_ALL) : 0);
    }

}
