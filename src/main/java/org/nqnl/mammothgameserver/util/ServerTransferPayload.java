package org.nqnl.mammothgameserver.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.minecraft.server.v1_16_R1.MojangsonParser;
import net.minecraft.server.v1_16_R1.NBTTagCompound;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftEntity;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class ServerTransferPayload {
    public static String createPayload(Player player) {
        HashMap<String, Object> playerData = new HashMap<String, Object>();
        String[] inventoryStrings = PlayerDataSerialize.playerInventoryToBase64(player.getInventory());
        playerData.put("inventory", inventoryStrings[0]);
        playerData.put("armor", inventoryStrings[1]);
        playerData.put("heldslot", player.getInventory().getHeldItemSlot());
        playerData.put("hunger", player.getFoodLevel());
        playerData.put("health", player.getHealth());
        playerData.put("xp", ExperienceManager.getTotalExperience(player));
        playerData.put("pitch", player.getLocation().getPitch());
        playerData.put("yaw", player.getLocation().getYaw());
        playerData.put("x", player.getLocation().getX());
        playerData.put("y", player.getLocation().getY());
        playerData.put("z", player.getLocation().getZ());
        playerData.put("world", player.getWorld().getName());
        playerData.put("isGliding", player.isGliding());

        // get speed for better elytra flight
        Vector velocity = player.getVelocity();
        String velocityString = velocity.getX() + "," + velocity.getY() + "," + velocity.getZ();
        playerData.put("velocity", velocityString);

        Collection<PotionEffect> potionEffects = player.getActivePotionEffects();
        // holy fucking shit java sucks
        Iterator<PotionEffect> iterator = potionEffects.iterator();
        // honestly this is probably faster than serializing a set of hashmaps.
        String potionString = "";
        while (iterator.hasNext()) {
            PotionEffect effect = iterator.next();
            potionString += (effect.getType().getName() + ",");
            potionString += ((Integer) effect.getDuration()).toString() + ",";
            potionString += ((Integer) effect.getAmplifier()).toString() + ",";
        }

        playerData.put("potions", potionString);


        // is the player riding a horse
        if (player.isInsideVehicle() && player.getVehicle() instanceof Horse) {
            Horse horse = (Horse) player.getVehicle();
            playerData.put("horse", getNBT(horse));
            horse.getInventory().setArmor(null);
            horse.getInventory().setSaddle(null);
            horse.remove();
        }
        // check for boat
        if (player.isInsideVehicle() && player.getVehicle() instanceof Boat) {
            Boat boat = (Boat) player.getVehicle();
            playerData.put("boat", getNBT(boat));
            boat.remove();
        }
        // check for nether strider
        if (player.isInsideVehicle() && player.getVehicle() instanceof Strider) {
            Strider strider = (Strider) player.getVehicle();
            playerData.put("strider", getNBT(strider));
            strider.remove();
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            String playerAsJson = mapper.writeValueAsString(playerData);
            return playerAsJson;
        } catch (Exception e) {
            e.printStackTrace();
            return "err";
        }
    }
    public static String getNBT(Entity e) {
        net.minecraft.server.v1_16_R1.Entity nms = ((CraftEntity) e).getHandle();
        NBTTagCompound nbt = new NBTTagCompound();
        nms.d(nbt);
        return nbt.toString();
    }
    public static void setNBT(Entity e, String value) {
        net.minecraft.server.v1_16_R1.Entity nms = ((CraftEntity) e).getHandle();
        try {
            NBTTagCompound nbtv = MojangsonParser.parse(value);
            nms.load(nbtv);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
