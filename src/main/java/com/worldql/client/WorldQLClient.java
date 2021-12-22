package com.worldql.client;

import com.worldql.client.listeners.NotImplementedCanceller;
import com.worldql.client.listeners.OutgoingPlayerHitListener;
import com.worldql.client.listeners.chunks.ChunkLoadEventListener;
import com.worldql.client.listeners.chunks.ChunkUnloadEventListener;
import com.worldql.client.listeners.explosions.EntityExplodeEventListener;
import com.worldql.client.listeners.explosions.ExplosionPrimeEventListener;
import com.worldql.client.listeners.explosions.TNTPrimeEventListener;
import com.worldql.client.listeners.player.*;
import com.worldql.client.listeners.world.InvestoryMoveEventListener;
import com.worldql.client.listeners.world.PlayerBreakBlockListener;
import com.worldql.client.listeners.world.PlayerEditSignListener;
import com.worldql.client.listeners.world.PlayerPlaceBlockListener;
import com.worldql.client.protocols.ProtocolManager;
import com.worldql.client.worldql_serialization.Instruction;
import com.worldql.client.worldql_serialization.Message;
import org.bukkit.Bukkit;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import redis.clients.jedis.JedisPool;

import java.util.UUID;


public class WorldQLClient extends JavaPlugin {
    public static WorldQLClient pluginInstance;
    public static UUID worldQLClientId;
    public static JedisPool pool;
    public static int mammothServerId;
    private Thread zeroMQThread;
    private ZContext context;
    private ZMQ.Socket pushSocket;
    private PacketReader packetReader;

    @Override
    public void onEnable() {
        pluginInstance = this;
        getLogger().info("Initializing Mammoth WorldQL client v0.3");
        saveDefaultConfig();

        String worldqlHost = getConfig().getString("worldql.host", "127.0.0.1");
        int worldqlPushPort = getConfig().getInt("worldql.push-port", 5555);

        mammothServerId = Bukkit.getServer().getPort() - getConfig().getInt("starting-port");
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        Slices.enabled = getConfig().getBoolean("slice-mode");
        if (Slices.enabled) {
            Slices.numServers = getConfig().getInt("num-servers");
            Slices.worldDiameter = getConfig().getInt("world-diameter");
            Slices.sliceWidth = getConfig().getInt("slice-width");
            Slices.dmzSize = getConfig().getInt("dmz-size");

            WorldBorder wb = Bukkit.getWorld("world").getWorldBorder();
            wb.setCenter(0,0);
            wb.setSize(getConfig().getInt("world-diameter"));
        }

        worldQLClientId = java.util.UUID.randomUUID();

        context = new ZContext();
        pushSocket = context.createSocket(SocketType.PUSH);
        packetReader = new PacketReader();
        getLogger().info("Attempting to connect to WorldQL server.");
        pushSocket.connect("tcp://%s:%d".formatted(worldqlHost, worldqlPushPort));

        pool = new JedisPool("localhost", 6379);

        String selfHostname = getConfig().getString("host", "127.0.0.1");

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                Message message = new Message(
                        Instruction.Heartbeat,
                        WorldQLClient.worldQLClientId,
                        "@global"
                );

                pushSocket.send(message.encode(), zmq.ZMQ.ZMQ_DONTWAIT);
            }
        }, 0L, 20L * 5L);


        // Initialize Protocol
        ProtocolManager.read();

        // For syncing player movements
        getServer().getPluginManager().registerEvents(new PlayerMoveAndLookHandler(), this);
        getServer().getPluginManager().registerEvents(new PlayerCrouchListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractEventListener(), this);
        getServer().getPluginManager().registerEvents(new ChunkLoadEventListener(), this);
        getServer().getPluginManager().registerEvents(new ChunkUnloadEventListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerHeldItemListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerArmorEditListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerShieldInteractListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerTeleportEventListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerServerTransferJoinLeave(), this);
        // Sync broken and placed blocks.
        getServer().getPluginManager().registerEvents(new PlayerBreakBlockListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerPlaceBlockListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerEditSignListener(), this);

        getServer().getPluginManager().registerEvents(new TNTPrimeEventListener(), this);
        getServer().getPluginManager().registerEvents(new ExplosionPrimeEventListener(), this);
        getServer().getPluginManager().registerEvents(new EntityExplodeEventListener(), this);

        getServer().getPluginManager().registerEvents(new OutgoingPlayerHitListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerShootBowListener(), this);

        getServer().getPluginManager().registerEvents(new PlayerChatListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);

        getServer().getPluginManager().registerEvents(new NotImplementedCanceller(), this);

        getServer().getPluginManager().registerEvents(new InvestoryMoveEventListener(), this);

        /*

        getServer().getPluginManager().registerEvents(new PortalCreateEventListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerLoadChunkListener(), this);
        getServer().getPluginManager().registerEvents(new OutgoingPlayerHitListener(), this);
        getServer().getPluginManager().registerEvents(new BlockDropItemEventListener(), this);

         */

        zeroMQThread = new Thread(new ZeroMQServer(this, context, selfHostname));
        zeroMQThread.start();
    }

    @Override
    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            PlayerServerTransferJoinLeave.savePlayerToRedis(player);
        }

        getLogger().info("Shutting down ZeroMQ thread.");
        context.close();
        try {
            zeroMQThread.interrupt();
            zeroMQThread.join();
        } catch (InterruptedException ignored) {
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
