package com.worldql.client.listeners.player;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.CrossDirection;
import com.worldql.client.Slices;
import com.worldql.client.WorldQLClient;
import com.worldql.client.ghost.PlayerGhostManager;
import com.worldql.client.minecraft_serialization.ExperienceUtil;
import com.worldql.client.minecraft_serialization.PlayerDataSerialize;
import com.worldql.client.protocols.ProtocolManager;
import com.worldql.client.worldql_serialization.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.nbt.MojangsonParser;
import net.minecraft.nbt.NBTTagCompound;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import redis.clients.jedis.Jedis;
import zmq.ZMQ;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PlayerServerTransferJoinLeave implements Listener {
    @EventHandler
    public void onPlayerLogOut(PlayerQuitEvent e) {
        savePlayerToRedis(e.getPlayer());

        if (WorldQLClient.getPluginInstance().getConfig().getBoolean("inventory-sync-only")) {
            return;
        }

        if (ProtocolManager.isinjected(e.getPlayer()))
            ProtocolManager.uninjectPlayer(e.getPlayer());
        // Send quit event to other clients
        FlexBuffersBuilder b = Codec.getFlexBuilder();
        int pmap = b.startMap();
        b.putString("username", e.getPlayer().getName());
        b.putString("uuid", e.getPlayer().getUniqueId().toString());
        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();
        Message message = new Message(
                Instruction.LocalMessage,
                WorldQLClient.worldQLClientId,
                e.getPlayer().getWorld().getName(),
                Replication.ExceptSelf,
                new Vec3D(e.getPlayer().getLocation()),
                null,
                null,
                "MinecraftPlayerQuit",
                bb
        );
        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
    }

    @EventHandler
    public void onPlayerLogIn(PlayerJoinEvent e) {
        Jedis j = WorldQLClient.pool.getResource();
        String data = j.get("player-" + e.getPlayer().getUniqueId());
        WorldQLClient.pool.returnResource(j);

        if (data != null) {
            try {
                setPlayerData(data, e.getPlayer());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

        if (WorldQLClient.getPluginInstance().getConfig().getBoolean("inventory-sync-only")) {
            return;
        }

        Location playerLocation = e.getPlayer().getLocation();

        int locationOwner = Slices.getOwnerOfLocation(playerLocation);

        if (locationOwner != WorldQLClient.mammothServerId) {
            String cooldownKey = "cooldown-" + e.getPlayer().getUniqueId();

            if (j.exists(cooldownKey)) {
                e.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.GOLD + "You must wait before crossing server borders again!"));
                CrossDirection shoveDirection = Slices.getShoveDirection(playerLocation);
                if (shoveDirection == CrossDirection.ERROR) {
                    e.getPlayer().kickPlayer("The Mammoth server responsible for your region of the world is inaccessible.");
                }
                WorldQLClient.pool.returnResource(j);
                return;
            }

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF("mammoth_" + locationOwner);
            e.getPlayer().sendPluginMessage(WorldQLClient.getPluginInstance(), "BungeeCord", out.toByteArray());

            j.set(cooldownKey, "true");
            j.expire(cooldownKey, 5);

            WorldQLClient.pool.returnResource(j);
            return;
        }
        WorldQLClient.pool.returnResource(j);

        //WorldQLClient.logger.info("Setting player " + e.getPlayer().getDisplayName() + " to get ghost join packets sent.");
        ProtocolManager.injectPlayer(e.getPlayer());
        Player player = e.getPlayer();

        PlayerGhostManager.ensurePlayerHasJoinPackets(player.getUniqueId());

        FlexBuffersBuilder b = Codec.getFlexBuilder();
        int pmap = b.startMap();
        b.putFloat("pitch", player.getLocation().getPitch());
        b.putFloat("yaw", player.getLocation().getYaw());
        b.putString("username", player.getName());
        b.putString("uuid", player.getUniqueId().toString());
        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();

        Message message = new Message(
                Instruction.LocalMessage,
                WorldQLClient.worldQLClientId,
                e.getPlayer().getWorld().getName(),
                Replication.ExceptSelf,
                new Vec3D(player.getLocation()),
                null,
                null,
                "MinecraftPlayerMove",
                bb
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
    }


    public static void savePlayerToRedis(Player player) {
        Jedis j = WorldQLClient.pool.getResource();
        HashMap<String, Object> oldPlayerData = new HashMap<String, Object>();
        Boolean hasOldData = false;

        if (j.exists("player-" + player.getUniqueId())) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                oldPlayerData = mapper.readValue(j.get("player-" + player.getUniqueId()), new TypeReference<Map<String, Object>>() {
                });
                if (WorldQLClient.getPluginInstance().getConfig().getBoolean("inventory-sync-only")) {
                    hasOldData = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        HashMap<String, Object> playerData = new HashMap<String, Object>();
        String[] inventoryStrings = PlayerDataSerialize.playerInventoryToBase64(player.getInventory());
        playerData.put("inventory", inventoryStrings[0]);
        playerData.put("armor", inventoryStrings[1]);
        playerData.put("heldslot", player.getInventory().getHeldItemSlot());
        playerData.put("hunger", player.getFoodLevel());
        playerData.put("health", player.getHealth());
        playerData.put("xp", ExperienceUtil.getTotalExperience(player));
        playerData.put("pitch", player.getLocation().getPitch());
        playerData.put("yaw", player.getLocation().getYaw());
        if (hasOldData) {
            playerData.put("x", oldPlayerData.get("x"));
            playerData.put("y", oldPlayerData.get("y"));
            playerData.put("z", oldPlayerData.get("z"));
        } else {
            playerData.put("x", player.getLocation().getX());
            playerData.put("y", player.getLocation().getY());
            playerData.put("z", player.getLocation().getZ());
        }
        playerData.put("world", player.getWorld().getName());
        playerData.put("isGliding", player.isGliding());

        // get speed for better elytra flight
        Vector velocity = player.getVelocity();
        String velocityString = velocity.getX() + "," + velocity.getY() + "," + velocity.getZ();
        playerData.put("velocity", velocityString);

        Collection<PotionEffect> potionEffects = player.getActivePotionEffects();
        Iterator<PotionEffect> iterator = potionEffects.iterator();
        StringBuilder potionString = new StringBuilder();
        while (iterator.hasNext()) {
            PotionEffect effect = iterator.next();
            potionString.append(effect.getType().getName()).append(",");
            potionString.append(((Integer) effect.getDuration())).append(",");
            potionString.append(((Integer) effect.getAmplifier())).append(",");
        }
        playerData.put("potions", potionString.toString());


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
            j.set("player-" + player.getUniqueId(), playerAsJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
        WorldQLClient.pool.returnResource(j);
    }

    private static String getNBT(Entity e) {
        net.minecraft.world.entity.Entity nms = ((CraftEntity) e).getHandle();
        NBTTagCompound nbt = new NBTTagCompound();
        nms.e(nbt);
        return nbt.toString();
    }

    private static void setNBT(Entity e, String value) {
        net.minecraft.world.entity.Entity nms = ((CraftEntity) e).getHandle();
        try {
            NBTTagCompound nbtv = MojangsonParser.a(value);
            nms.g(nbtv);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void setPlayerData(String playerJSON, Player player) throws IOException {
        HashMap<String, Object> playerData = new HashMap<String, Object>();
        ObjectMapper mapper = new ObjectMapper();
        playerData = mapper.readValue(playerJSON, new TypeReference<Map<String, Object>>() {
        });

        // teleport the player to the right place.
        World w = player.getServer().getWorld((String) playerData.get("world"));
        if (!WorldQLClient.getPluginInstance().inventorySyncOnly && playerData.get("x") != null) {
            Location loc = new Location(w, (Double) playerData.get("x"), (Double) playerData.get("y"), (Double) playerData.get("z"),
                    (float) (double) (Double) playerData.get("yaw"), (float) (double) (Double) playerData.get("pitch"));

            player.teleport(loc);
        }

        // set inventory and player stats
        if (WorldQLClient.getPluginInstance().loadPlayerData) {
            ItemStack[] playerInventory = PlayerDataSerialize.itemStackArrayFromBase64((String) playerData.get("inventory"));
            ItemStack[] armorContents = PlayerDataSerialize.itemStackArrayFromBase64((String) playerData.get("armor"));
            player.getInventory().setContents(playerInventory);
            player.getInventory().setArmorContents(armorContents);

            ExperienceUtil.setTotalExperience(player, (Integer) playerData.get("xp"));
            player.setFoodLevel((Integer) playerData.get("hunger"));
            player.setHealth((Double) playerData.get("health"));
            player.getInventory().setHeldItemSlot((Integer) playerData.get("heldslot"));
        }

        // set velocity
        String[] velocityComponents = ((String) playerData.get("velocity")).split(",");
        Vector velocity = new Vector(Double.parseDouble(velocityComponents[0]), Double.parseDouble(velocityComponents[1]),
                Double.parseDouble(velocityComponents[2]));
        player.setVelocity(velocity);

        if (!WorldQLClient.getPluginInstance().inventorySyncOnly) {
            if (playerData.containsKey("horse")) {
                Entity newHorse = player.getWorld().spawnEntity(player.getLocation(), EntityType.HORSE);
                setNBT(newHorse, (String) playerData.get("horse"));
                newHorse.addPassenger(player);
            }
            if (playerData.containsKey("boat")) {
                Entity newBoat = player.getWorld().spawnEntity(player.getLocation(), EntityType.BOAT);
                setNBT(newBoat, (String) playerData.get("boat"));
                newBoat.addPassenger(player);
            }
            if (playerData.containsKey("strider")) {
                Entity newStrider = player.getWorld().spawnEntity(player.getLocation(), EntityType.STRIDER);
                setNBT(newStrider, (String) playerData.get("strider"));
                newStrider.addPassenger(player);
            }
        }

        player.setGliding((boolean) playerData.get("isGliding"));

        String potions[] = ((String) playerData.get("potions")).split(",");
        // remove all potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        if (potions.length > 1) {
            // loop through effects and apply them.
            int c = 0;
            while (c < potions.length) {
                PotionEffectType effectType = PotionEffectType.getByName(potions[c]);
                c++;
                int duration = Integer.parseInt(potions[c]);
                c++;
                int amplifier = Integer.parseInt(potions[c]);
                PotionEffect potionEffect = effectType.createEffect(duration, amplifier);
                player.addPotionEffect(potionEffect);
                c++;
            }
        }
    }
}
