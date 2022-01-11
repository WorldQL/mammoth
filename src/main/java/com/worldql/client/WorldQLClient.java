package com.worldql.client;

import com.worldql.client.listeners.NotImplementedCanceller;
import com.worldql.client.listeners.OutgoingPlayerHitListener;
import com.worldql.client.listeners.chunks.ChunkLoadEventListener;
import com.worldql.client.listeners.chunks.ChunkUnloadEventListener;
import com.worldql.client.listeners.explosions.BlockExplodeEventListener;
import com.worldql.client.listeners.explosions.EntityExplodeEventListener;
import com.worldql.client.listeners.explosions.ExplosionPrimeEventListener;
import com.worldql.client.listeners.explosions.TNTPrimeEventListener;
import com.worldql.client.listeners.player.*;
import com.worldql.client.listeners.world.*;
import com.worldql.client.minecraft_serialization.SaveLoadPlayerFromRedis;
import com.worldql.client.protocols.ProtocolManager;
import com.worldql.client.worldql_serialization.Instruction;
import com.worldql.client.worldql_serialization.Message;
import com.worldql.client.worldql_serialization.Replication;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class WorldQLClient extends JavaPlugin {
    public static boolean disabling;
    public static WorldQLClient pluginInstance;
    public static UUID worldQLClientId;
    public static JedisPool pool;
    public static int mammothServerId;
    private Thread zeroMQThread;
    private ZContext context;
    private ZMQ.Socket pushSocket;
    private PacketReader packetReader;
    public static boolean processGhosts;
    public static boolean syncPlayerInventory;
    public static boolean syncPlayerHealthXPHunger;
    public static boolean syncPlayerEffects;
    public static PlayerDataSavingManager playerDataSavingManager;
    public static long timestampOfLastHeartbeat;
    static int zeroMQServerPort;

    @Override
    public void onEnable() {
        disabling = false;
        pluginInstance = this;
        getLogger().info("Initializing Mammoth v0.71");
        saveDefaultConfig();

        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(128);
        pool = new JedisPool(jedisPoolConfig, getConfig().getString("redis.host"), getConfig().getInt("redis.port"));
        // Make sure we're connected
        try (Jedis j = pool.getResource()) {
            j.ping();
            getLogger().info("Redis connection successful.");
        } catch (JedisConnectionException e) {
            getLogger().warning("Failed to connect to Redis. Both WorldQL and Redis are required for Mammoth. Stopping server...");
            Bukkit.getServer().shutdown();
            return;
        }

        mammothServerId = Bukkit.getServer().getPort() - getConfig().getInt("starting-port");
        worldQLClientId = java.util.UUID.randomUUID();
        context = new ZContext();
        pushSocket = context.createSocket(SocketType.PUSH);
        packetReader = new PacketReader();
        processGhosts = getConfig().getBoolean("ghosts", false);
        syncPlayerInventory = getConfig().getBoolean("sync-player-inventory", true);
        syncPlayerHealthXPHunger = getConfig().getBoolean("sync-player-health-xp-hunger", true);
        syncPlayerEffects = getConfig().getBoolean("sync-player-effects", true);
        playerDataSavingManager = new PlayerDataSavingManager();
        timestampOfLastHeartbeat = Instant.now().toEpochMilli();

        String worldqlHost = getConfig().getString("worldql.host", "127.0.0.1");
        int worldqlPushPort = getConfig().getInt("worldql.push-port", 5555);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        String selfHostname = getConfig().getString("host", "127.0.0.1");

        // Connect to the WorldQL server.
        getLogger().info("Attempting to connect to WorldQL server.");
        pushSocket.connect("tcp://%s:%d".formatted(worldqlHost, worldqlPushPort));

        Slices.enabled = getConfig().getBoolean("slice-mode");
        if (Slices.enabled) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Slices.numServers = getConfig().getInt("num-servers");
                Slices.worldDiameter = getConfig().getInt("world-diameter");
                Slices.sliceWidth = getConfig().getInt("slice-width");
                Slices.dmzSize = getConfig().getInt("dmz-size");
                int worldDiameter = getConfig().getInt("world-diameter");
                try {
                    World world = Bukkit.getWorld("world");
                    World nether = Bukkit.getWorld("world_nether");
                    World end = Bukkit.getWorld("world_the_end");

                    WorldBorder wb = world.getWorldBorder();
                    wb.setCenter(0, 0);
                    wb.setSize(worldDiameter);

                    wb = nether.getWorldBorder();
                    wb.setCenter(0, 0);
                    wb.setSize(worldDiameter / 8 - 10);

                    wb = end.getWorldBorder();
                    wb.setCenter(0, 0);
                    wb.setSize(worldDiameter);

                    world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
                    nether.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
                    end.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 20);
        }

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                Message message = new Message(
                        Instruction.Heartbeat,
                        WorldQLClient.worldQLClientId,
                        "@global"
                );

                pushSocket.send(message.encode(), zmq.ZMQ.ZMQ_DONTWAIT);

                long now = Instant.now().toEpochMilli();
                if (now - timestampOfLastHeartbeat > 15000) {
                    getLogger().warning("Haven't received a heartbeat from WorldQL in over 15 seconds! Attempting to reconnect.");
                    Message reconnectMessage = new Message(
                            Instruction.Handshake,
                            WorldQLClient.worldQLClientId,
                            "@global",
                            Replication.ExceptSelf,
                            null,
                            null,
                            null,
                            selfHostname + ":" + WorldQLClient.zeroMQServerPort,
                            null
                    );

                    WorldQLClient.getPluginInstance().getPushSocket().send(reconnectMessage.encode(), ZMQ.DONTWAIT);
                }
            }
        }, 5L, 20L * 5L);

        // Initialize Protocol
        ProtocolManager.read();

        // TODO: Remove this command after we figure out the cause of players being spawned in the ground.
        getCommand("unstuck").setExecutor(new CommandUnstuck());

        getServer().getPluginManager().registerEvents(new PlayerServerTransferJoinLeave(), this);
        // Handles server transfers and the movement component of ghosts.
        getServer().getPluginManager().registerEvents(new PlayerMoveAndLookHandler(), this);

        // For ghosts.
        if (processGhosts) {
            getServer().getPluginManager().registerEvents(new PlayerCrouchListener(), this);
            getServer().getPluginManager().registerEvents(new PlayerInteractEventListener(), this);
            getServer().getPluginManager().registerEvents(new PlayerArmorEditListener(), this);
            getServer().getPluginManager().registerEvents(new PlayerHeldItemListener(), this);
            getServer().getPluginManager().registerEvents(new PlayerShieldInteractListener(), this);
            getServer().getPluginManager().registerEvents(new PlayerTeleportEventListener(), this);
            getServer().getPluginManager().registerEvents(new PlayerShootBowListener(), this);
        }

        // To sub/unsub from regions of the world.
        if (processGhosts || !Slices.enabled) {
            getServer().getPluginManager().registerEvents(new ChunkLoadEventListener(), this);
            getServer().getPluginManager().registerEvents(new ChunkUnloadEventListener(), this);
        }

        // Sync broken and placed blocks.
        getServer().getPluginManager().registerEvents(new PlayerBreakBlockListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerPlaceBlockListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerEditSignListener(), this);
        getServer().getPluginManager().registerEvents(new PortalCreateEventListener(), this);

        // For explosions.
        getServer().getPluginManager().registerEvents(new TNTPrimeEventListener(), this);
        getServer().getPluginManager().registerEvents(new ExplosionPrimeEventListener(), this);
        getServer().getPluginManager().registerEvents(new EntityExplodeEventListener(), this);
        getServer().getPluginManager().registerEvents(new BlockExplodeEventListener(), this);

        // Chat sync
        getServer().getPluginManager().registerEvents(new PlayerChatListener(), this);
        // Death drop mechanics.
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);

        // Cancel events that can cause desync in any mode.
        getServer().getPluginManager().registerEvents(new NotImplementedCanceller(), this);
        getServer().getPluginManager().registerEvents(new PlayerDropItemListener(), this);
        // Custom listeners.
        getServer().getPluginManager().registerEvents(new OutgoingPlayerHitListener(), this);

        zeroMQThread = new Thread(new ZeroMQServer(this, context, selfHostname));
        zeroMQThread.start();
    }

    @Override
    public void onDisable() {
        disabling = true;
        for (Player player : getServer().getOnlinePlayers()) {
            SaveLoadPlayerFromRedis.savePlayerToRedis(player);
        }
        if (context != null && zeroMQThread != null) {
            getLogger().info("Shutting down ZeroMQ thread.");
            context.close();
            try {
                zeroMQThread.interrupt();
                zeroMQThread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }

    public static WorldQLClient getPluginInstance() {
        return pluginInstance;
    }

    public PacketReader getPacketReader() {
        return packetReader;
    }

    public ZMQ.Socket getPushSocket() {
        return pushSocket;
    }
}
