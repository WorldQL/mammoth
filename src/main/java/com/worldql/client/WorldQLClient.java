package com.worldql.client;

import com.worldql.client.serialization.Codec;
import com.worldql.client.serialization.Instruction;
import com.worldql.client.serialization.Message;
import org.bukkit.Bukkit;

import com.worldql.client.listeners.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.UUID;


public class WorldQLClient extends JavaPlugin {
    public static WorldQLClient pluginInstance;
    public static UUID worldQLClientId;
    private Thread zeroMQThread;
    private ZContext context;
    private ZMQ.Socket pushSocket;
    private int zmqPortClientId;
    private PacketReader packetReader;

    @Override
    public void onEnable() {
        pluginInstance = this;
        getLogger().info("Initializing Mammoth WorldQL client.");
        saveDefaultConfig();

        String worldqlHost = getConfig().getString("worldql.host", "127.0.0.1");
        int worldqlPushPort = getConfig().getInt("worldql.push-port", 5555);
        worldQLClientId = java.util.UUID.randomUUID();

        context = new ZContext();
        pushSocket = context.createSocket(SocketType.PUSH);
        packetReader = new PacketReader();
        getLogger().info("Attempting to connect to WorldQL server.");
        pushSocket.connect("tcp://%s:%d".formatted(worldqlHost, worldqlPushPort));

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


        getServer().getPluginManager().registerEvents(new PlayerMoveAndLookHandler(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinEventListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerCrouchListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractEventListener(), this);
        getServer().getPluginManager().registerEvents(new ChunkLoadEventListener(), this);
        getServer().getPluginManager().registerEvents(new ChunkUnloadEventListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerHeldItemListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerArmorEditListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerShieldInteractListener(), this);
        /*
        getServer().getPluginManager().registerEvents(new PlayerLogOutListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerBlockPlaceListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerEditSignListener(), this);
        getServer().getPluginManager().registerEvents(new PortalCreateEventListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerLoadChunkListener(), this);
        getServer().getPluginManager().registerEvents(new OutgoingPlayerHitListener(), this);
        getServer().getPluginManager().registerEvents(new BlockDropItemEventListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerTeleportEventListener(), this);

         */

        this.getCommand("refreshworld").setExecutor(new TestRefreshWorldCommand());

        zeroMQThread = new Thread(new ZeroMQServer(this, context, selfHostname));
        zeroMQThread.start();
    }

    @Override
    public void onDisable() {
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

    public int getZmqPortClientId() {
        return zmqPortClientId;
    }
}
