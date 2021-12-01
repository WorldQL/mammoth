package com.worldql.client.listeners;

import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.events.OutgoingPlayerHitEvent;
import com.worldql.client.serialization.*;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import zmq.ZMQ;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

import java.nio.ByteBuffer;

public class OutgoingPlayerHitListener implements Listener {

    @EventHandler
    public void onPlayerHit(OutgoingPlayerHitEvent event) {
        FlexBuffersBuilder b = Codec.getFlexBuilder();
        int pmap = b.startMap();

        b.putBoolean("sprinting", event.getAttacker().isSprinting());
        b.putInt("knockbacklvl", getKnockBackLevel(event.getAttacker()));
        b.putFloat("damage", getDamageAmount(event.getAttacker()));
        b.putString("username", event.getReceiver().co());
        b.putString("uuid", event.getUUID().toString());
        b.putString("uuidofattacker", event.getAttacker().getUniqueId().toString());
        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();

        Message message = new Message(
                Instruction.LocalMessage,
                WorldQLClient.worldQLClientId,
                event.getAttacker().getWorld().getName(),
                Replication.ExceptSelf,
                new Vec3D(new Location(event.getAttacker().getWorld(),
                        // x, y, z
                        event.getReceiver().dc(), event.getReceiver().de(), event.getReceiver().di())),
                null,
                null,
                "MinecraftPlayerDamage",
                bb
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);

    }

    // added support for arrows, if we can get those to work.
    private static int getKnockBackLevel(Entity entity){
        if(entity instanceof LivingEntity) {
            ItemStack item = ((LivingEntity) entity).getEquipment().getItemInMainHand();
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasEnchants() && item.getItemMeta().hasEnchant(Enchantment.KNOCKBACK))
                return item.getItemMeta().getEnchantLevel(Enchantment.KNOCKBACK);
            else
                return 0;
        }
        else if(entity instanceof AbstractArrow arrow)
            return arrow.getKnockbackStrength();
        else
            return 0;

    }


    private static double getDamageAmount(Player player) {
        if (player.getInventory().getItemInMainHand() == null)
            return 1;
        return player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
    }

}
