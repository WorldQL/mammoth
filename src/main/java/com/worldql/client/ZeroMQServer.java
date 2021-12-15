package com.worldql.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.worldql.client.ghost.PlayerGhostManager;
import com.worldql.client.listeners.player.PlayerChatListener;
import com.worldql.client.listeners.player.PlayerDeathListener;
import com.worldql.client.listeners.player.PlayerLogOutListener;
import com.worldql.client.listeners.utils.BlockTools;
import com.worldql.client.serialization.Instruction;
import com.worldql.client.serialization.Message;
import com.worldql.client.serialization.Replication;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.RegionAccessor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ZeroMQServer implements Runnable {
    private final Plugin plugin;
    private final ZContext context;
    private final String hostname;

    public ZeroMQServer(Plugin plugin, ZContext context, String hostname) {
        this.plugin = plugin;
        this.context = context;
        this.hostname = hostname;
    }

    @Override
    public void run() {
        ZMQ.Socket socket = context.createSocket(SocketType.PULL);
        int port = socket.bindToRandomPort("tcp://" + hostname, 29000, 30000);

        Message message = new Message(
                Instruction.Handshake,
                WorldQLClient.worldQLClientId,
                "@global",
                Replication.ExceptSelf,
                null,
                null,
                null,
                hostname + ":" + port,
                null
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.DONTWAIT);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] reply = socket.recv(0);
                java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(reply);
                var incoming = Message.decode(buf);
                boolean isSelf = incoming.senderUuid().equals(WorldQLClient.worldQLClientId);

                if (incoming.instruction() == Instruction.Handshake) {
                    WorldQLClient.getPluginInstance().getLogger().info("Response from WorldQL handshake: " + incoming.parameter());
                    continue;
                }

                if (incoming.instruction() == Instruction.GlobalMessage) {
                    if (incoming.parameter().equals("MinecraftPlayerChat")) {
                        PlayerChatListener.relayChat(incoming);
                    }

                    if (incoming.parameter().equals("MinecraftPlayerDeath")) {
                        PlayerDeathListener.handleIncomingDeath(incoming, isSelf);
                    }

                    if (incoming.parameter().equals("WorldGuardPlayerClaimRegion")) {
                        WorldQLClient.getPluginInstance().getLogger().info("Incoming region claim message.");
                        String json = StandardCharsets.UTF_8.decode(incoming.flex()).toString();

                        Gson gson = new Gson();
                        JsonObject o = gson.fromJson(json, JsonObject.class);
                        String messageToBroadcast = o.get("broadcast_message").getAsString();
                        try {
                            World world = Bukkit.getWorld(incoming.worldName());
                            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
                            RegionManager regions = container.get(BukkitAdapter.adapt(world));
                            int minX = o.get("min_x").getAsInt();
                            int maxX = o.get("max_x").getAsInt();
                            int minZ = o.get("min_z").getAsInt();
                            int maxZ = o.get("max_z").getAsInt();
                            int minHeight = world.getMinHeight();
                            int maxHeight = world.getMaxHeight();
                            String regionName = String.format("%s-%s-%s-%s", minX, maxX, minZ, maxZ);

                            ProtectedRegion existing = regions.getRegion(regionName);
                            if (existing != null) {
                                existing.getOwners().clear();
                                existing.getOwners().addPlayer(UUID.fromString(o.get("owner_uuid").getAsString()));
                                return;
                            }
                            Bukkit.broadcastMessage(messageToBroadcast);


                            BlockVector3 min = BlockVector3.at(minX, minHeight, minZ);
                            BlockVector3 max = BlockVector3.at(maxX, maxHeight, maxZ);


                            System.out.println(regionName);
                            ProtectedRegion region = new ProtectedCuboidRegion(regionName, min, max);
                            region.setPriority(3);
                            region.setFlag(Flags.GREET_TITLE, o.get("plot_name").getAsString());
                            region.setFlag(Flags.GREET_MESSAGE, o.get("plot_info").getAsString());
                            region.getOwners().addPlayer(UUID.fromString(o.get("owner_uuid").getAsString()));

                            regions.addRegion(region);
                        } catch (Exception e) {
                            WorldQLClient.getPluginInstance().getLogger().warning("Failed to process WorldGuard message " +
                                    "because dependencies are not installed.");
                        }

                    }
                }

                if (incoming.instruction() == Instruction.LocalMessage) {
                    if (incoming.parameter().equals("MinecraftBlockUpdate")) {
                        BlockTools.setRecords(incoming.records(), isSelf);
                        continue;
                    }
                    if (incoming.parameter().equals("MinecraftEndCrystalCreate")) {
                        BlockTools.createEndCrystal(incoming.position(), incoming.worldName());
                        continue;
                    }
                    if (incoming.parameter().startsWith("MinecraftPlayer")) {
                        PlayerGhostManager.updateNPC(incoming);
                    }
                    if (incoming.parameter().equals("MinecraftExplosion")) {
                        WorldQLClient.getPluginInstance().getLogger().info("Got incoming explosion");
                        BlockTools.createExplosion(incoming.position(), incoming.worldName());
                    }
                    if (incoming.parameter().equals("MinecraftPrimeTNT")) {
                        BlockTools.createPrimedTNT(incoming.position(), incoming.worldName());
                    }
                }

                if (incoming.instruction() == Instruction.RecordReply) {
                    if (incoming.worldName().equals("inventory")) {
                        PlayerLogOutListener.setInventories(incoming.records());
                    } else {
                        if (!incoming.records().isEmpty()) {
                            BlockTools.setRecords(incoming.records(), false);
                        }
                    }
                }

            } catch (Exception e) {
                if (e instanceof ZMQException) {
                    if (((ZMQException) e).getErrorCode() == ZMQ.Error.ETERM.getCode()) {
                        break;
                    }
                }
            }
        }
        socket.setLinger(0);
        socket.close();
    }


}
