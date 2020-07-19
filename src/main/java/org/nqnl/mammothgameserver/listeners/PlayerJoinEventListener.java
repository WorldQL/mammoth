package org.nqnl.mammothgameserver.listeners;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.nqnl.mammothgameserver.MammothGameserver;
import org.nqnl.mammothgameserver.util.ExperienceManager;
import org.nqnl.mammothgameserver.util.NetherMethods;
import org.nqnl.mammothgameserver.util.PlayerDataSerialize;
import org.nqnl.mammothgameserver.util.ServerTransferPayload;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;

import static org.nqnl.mammothgameserver.util.PlayerTransfer.STARTING_PORT;

public class PlayerJoinEventListener implements Listener {
    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        Jedis j = MammothGameserver.pool.getResource();
        j.set(Bukkit.getServer().getPort() - STARTING_PORT + "-playercount", Integer.toString(Bukkit.getServer().getOnlinePlayers().size()));
        Player player = event.getPlayer();
        try {
           String playerUuid = event.getPlayer().getUniqueId().toString();
            if (j.exists("player-"+playerUuid)) {
                String playerJSON = j.get("player-" + playerUuid);
                // deserialize it.
                HashMap<String, Object> playerData = new HashMap<String, Object>();
                ObjectMapper mapper = new ObjectMapper();
                playerData = mapper.readValue(playerJSON, new TypeReference<Map<String, Object>>(){});

                // teleport the player to the right place.
                World w = player.getServer().getWorld((String)playerData.get("world"));
                Location loc = new Location(w, (Double)playerData.get("x"), (Double)playerData.get("y"), (Double)playerData.get("z"),
                        (float)(double)(Double) playerData.get("yaw"), (float)(double)(Double) playerData.get("pitch"));
                player.teleport(loc);

                // set inventory and player stats
                ItemStack[] playerInventory = PlayerDataSerialize.itemStackArrayFromBase64((String)playerData.get("inventory"));
                ItemStack[] armorContents = PlayerDataSerialize.itemStackArrayFromBase64((String)playerData.get("armor"));
                player.getInventory().setContents(playerInventory);
                player.getInventory().setArmorContents(armorContents);
                ExperienceManager.setTotalExperience(player, (Integer)playerData.get("xp"));
                player.setFoodLevel((Integer)playerData.get("hunger"));
                player.setHealth((Double)playerData.get("health"));
                player.getInventory().setHeldItemSlot((Integer)playerData.get("heldslot"));

                // set velocity
                String[] velocityComponents = ((String) playerData.get("velocity")).split(",");
                Vector velocity = new Vector(Double.parseDouble(velocityComponents[0]), Double.parseDouble(velocityComponents[1]),
                        Double.parseDouble(velocityComponents[2]));
                player.setVelocity(velocity);

                if (playerData.containsKey("horse")) {
                    Entity newHorse = player.getWorld().spawnEntity(player.getLocation(), EntityType.HORSE);
                    ServerTransferPayload.setNBT(newHorse, (String)playerData.get("horse"));
                    newHorse.addPassenger(player);
                }



                player.setGliding((boolean)playerData.get("isGliding"));

                String potions[] = ((String)playerData.get("potions")).split(",");
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

                if (j.exists("portal-"+playerUuid)) {
                    String portalString = j.get("portal-"+playerUuid);
                    HashMap<String, Object> portalData = new HashMap<String, Object>();
                    portalData = mapper.readValue(portalString, new TypeReference<Map<String, Object>>(){});
                    if (portalData.get("toWorld").equals("world_the_end")) {
                        player.teleport(Bukkit.getWorld("world_the_end").getHighestBlockAt(new Location(Bukkit.getWorld("world_the_end"), 15, 30, 15)).getLocation());
                        j.del("portal-"+playerUuid);
                        return;
                    }
                    Location to = new Location(Bukkit.getWorld((String)portalData.get("toWorld")), (Double)portalData.get("toX"), (Double) portalData.get("toY"), (Double)portalData.get("toZ"));
                    Location tp = NetherMethods.findSafeLocation(to);
                    player.teleport(tp);
                    j.del("portal-"+playerUuid);
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            j.close();
        }
    }
}
