package org.nqnl.mammothgameserver.util;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.nqnl.mammothgameserver.MammothGameserver;
import org.nqnl.mammothgameserver.SliceMethods;
import redis.clients.jedis.Jedis;

public class PlayerTransfer {
    public static int STARTING_PORT = 26601;
    public static int END_SERVER = 0;
    public static int MAX_PLAYERS = 86;

    public static void handleNetherPortal(PlayerPortalEvent event, MammothGameserver instance) {
        Jedis j = MammothGameserver.pool.getResource();
        int currentServerId = Bukkit.getServer().getPort() - STARTING_PORT;
        event.setCancelled(true);
        try {
            if (event.getFrom().getWorld().getName().equals("world") && event.getTo().getWorld().getName().equals("world_nether")) {
                int portaledServerId = SliceMethods.getServerIdFromX(event.getTo().getX());
                if (j.exists(portaledServerId+"-playercount") && Integer.valueOf(j.get(portaledServerId+"-playercount")) >= MAX_PLAYERS) {
                    event.getPlayer().sendMessage(ChatColor.BLACK + "[" + ChatColor.GOLD + "SYSTEM" + ChatColor.BLACK + "] " + ChatColor.RESET + ChatColor.BLUE + "This region of the nether is full!");
                    return;
                }
                if (portaledServerId != currentServerId) {
                    String playerAsJson = ServerTransferPayload.createPayload(event.getPlayer());
                    String portalEventAsJson = PortalEventTransferPayload.createPortalPayload(event);
                    j.set("player-"+event.getPlayer().getUniqueId().toString(), playerAsJson);
                    j.set("portal-"+event.getPlayer().getUniqueId().toString(), portalEventAsJson);
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("Connect");
                    out.writeUTF("mammoth" + portaledServerId);
                    event.getPlayer().sendPluginMessage(instance, "BungeeCord", out.toByteArray());
                } else {
                    Location tp = NetherMethods.findSafeLocation(event.getTo());
                    event.getPlayer().teleport(tp);
                }
                return;
            }
            if (event.getFrom().getWorld().getName().equals("world_nether") && event.getTo().getWorld().getName().equals("world")) {
                int portaledServerId = SliceMethods.getServerIdFromX(event.getTo().getX());
                if (j.exists(portaledServerId+"-playercount") && Integer.valueOf(j.get(portaledServerId+"-playercount")) >= MAX_PLAYERS) {
                    event.getPlayer().sendMessage(ChatColor.BLACK + "[" + ChatColor.GOLD + "SYSTEM" + ChatColor.BLACK + "] " + ChatColor.RESET + ChatColor.BLUE + "This region of the world is full!");
                    return;
                }
                if (portaledServerId != currentServerId) {
                    String playerAsJson = ServerTransferPayload.createPayload(event.getPlayer());
                    String portalEventAsJson = PortalEventTransferPayload.createPortalPayload(event);
                    j.set("player-"+event.getPlayer().getUniqueId().toString(), playerAsJson);
                    j.set("portal-"+event.getPlayer().getUniqueId().toString(), portalEventAsJson);
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("Connect");
                    out.writeUTF("mammoth" + portaledServerId);
                    event.getPlayer().sendPluginMessage(instance, "BungeeCord", out.toByteArray());
                } else {
                    Location tp = NetherMethods.findSafeLocation(event.getTo());
                    event.getPlayer().teleport(tp);
                }
                return;
            }

            if (event.getFrom().getWorld().getName().equals("world") && event.getTo().getWorld().getName().equals("world_the_end")) {
                int portaledServerId = END_SERVER;
                if (j.exists(portaledServerId+"-playercount") && Integer.valueOf(j.get(portaledServerId+"-playercount")) >= MAX_PLAYERS) {
                    event.getPlayer().sendMessage(ChatColor.BLACK + "[" + ChatColor.GOLD + "SYSTEM" + ChatColor.BLACK + "] " + ChatColor.RESET + ChatColor.BLUE + "The end is full!");
                    return;
                }
                if (portaledServerId != currentServerId) {
                    String playerAsJson = ServerTransferPayload.createPayload(event.getPlayer());
                    String portalEventAsJson = PortalEventTransferPayload.createPortalPayload(event);
                    j.set("player-"+event.getPlayer().getUniqueId().toString(), playerAsJson);
                    j.set("portal-"+event.getPlayer().getUniqueId().toString(), portalEventAsJson);
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("Connect");
                    out.writeUTF("mammoth" + portaledServerId);
                    event.getPlayer().sendPluginMessage(instance, "BungeeCord", out.toByteArray());
                } else {
                    event.getPlayer().teleport(Bukkit.getWorld("world_the_end").getHighestBlockAt(new Location(Bukkit.getWorld("world_the_end"), 15, 30, 15)).getLocation());
                }
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            j.close();
        }


    }
    public static boolean transferPlayerIfNeeded(Location l, Cancellable event, Player player, MammothGameserver instance) {
        int currentServerId = Bukkit.getServer().getPort() - STARTING_PORT;
        int locationServerId = SliceMethods.getServerIdFromX(l.getX());

        if (player.getLocation().getWorld().getName().equals("world_the_end")) {
            locationServerId = END_SERVER;
        }

        if (currentServerId != locationServerId) {
            Jedis j = MammothGameserver.pool.getResource();
            try {
                if (!j.exists("cooldown-"+player.getUniqueId().toString())) {
                    // make sure the server isn't full
                    if (j.exists(locationServerId+"-playercount") && Integer.valueOf(j.get(locationServerId+"-playercount")) >= MAX_PLAYERS) {
                        int left = SliceMethods.getServerIdFromX(l.getX() - 5);
                        int right = SliceMethods.getServerIdFromX(l.getX() + 5);
                        if (left == currentServerId) {
                            player.teleport(new Location(player.getWorld(), player.getLocation().getX() - 2, player.getLocation().getY(), player.getLocation().getZ(), player.getLocation().getYaw(), player.getLocation().getPitch()));
                        }
                        if (right == currentServerId) {
                            player.teleport(new Location(player.getWorld(), player.getLocation().getX() + 2, player.getLocation().getY(), player.getLocation().getZ(), player.getLocation().getYaw(), player.getLocation().getPitch()));
                        }
                        player.sendMessage(ChatColor.BLACK + "[" + ChatColor.GOLD + "SYSTEM" + ChatColor.BLACK + "] " + ChatColor.RESET + ChatColor.BLUE + "This region of the world is full!");
                        j.set("cooldown-"+player.getUniqueId().toString(), "true");
                        j.expire("cooldown-"+player.getUniqueId().toString(), 15);
                        return true;
                    }
                    String playerAsJson = ServerTransferPayload.createPayload(player);
                    event.setCancelled(true);
                    j.set("player-" + player.getUniqueId().toString(), playerAsJson);
                    j.set("cooldown-"+player.getUniqueId().toString(), "true");
                    j.expire("cooldown-"+player.getUniqueId().toString(), 5);
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("Connect");
                    out.writeUTF("mammoth" + locationServerId);
                    player.sendPluginMessage(instance, "BungeeCord", out.toByteArray());
                    return true;
                } else {
                    // push the player into the right direction
                    if (event instanceof BlockPlaceEvent || event instanceof BlockBreakEvent) {
                        event.setCancelled(true);
                    }
                    int left = SliceMethods.getServerIdFromX(l.getX() - 5);
                    int right = SliceMethods.getServerIdFromX(l.getX() + 5);
                    if (left == right && Integer.valueOf(j.get(locationServerId+"-playercount")) >= MAX_PLAYERS) {
                        // the player has attempted to connect, kicked to the proxy, then forwarded to a full region of the world on a non-full server by the proxy
                        // this is a bad state and we have to kick the player because we cannot teleport them to a playable area.
                        player.kickPlayer("Your currently occupied region of the world is full. Please try again later.");
                        return true;
                    }
                    if (left == currentServerId) {
                        player.teleport(new Location(player.getWorld(), player.getLocation().getX() - 2, player.getLocation().getY(), player.getLocation().getZ(), player.getLocation().getYaw(), player.getLocation().getPitch()));
                    }
                    if (right == currentServerId) {
                        player.teleport(new Location(player.getWorld(), player.getLocation().getX() + 2, player.getLocation().getY(), player.getLocation().getZ(), player.getLocation().getYaw(), player.getLocation().getPitch()));
                    }
                    player.sendMessage(ChatColor.BLACK + "[" + ChatColor.GOLD + "SYSTEM" + ChatColor.BLACK + "] " + ChatColor.RESET + ChatColor.BLUE + "Please wait a few seconds before crossing a server boundary again.");
                    return true;
                }
            } catch (Exception e){
                e.printStackTrace();
            } finally {
                j.close();
            }
        }
        return false;
    }
}
