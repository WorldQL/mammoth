package com.worldql.client;

import org.bukkit.Bukkit;

import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.listeners.*;
import com.worldql.client.Messages.Instruction;
import com.worldql.client.Messages.Message;
import org.bukkit.plugin.java.JavaPlugin;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.Hashtable;


public class WorldQLClient extends JavaPlugin {
    public static WorldQLClient pluginInstance;
    public static String worldQLClientId;
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
        worldQLClientId = java.util.UUID.randomUUID().toString();

        context = new ZContext();
        pushSocket = context.createSocket(SocketType.PUSH);
        packetReader = new PacketReader();
        getLogger().info("Attempting to connect to WorldQL server.");
        pushSocket.connect("tcp://%s:%d".formatted(worldqlHost, worldqlPushPort));

        String selfHostname = getConfig().getString("host", "127.0.0.1");

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                FlatBufferBuilder builder = new FlatBufferBuilder(1024);

                int sender_uuid = builder.createString(worldQLClientId);
                int worldName = builder.createString("@global");

                Message.startMessage(builder);
                Message.addInstruction(builder, Instruction.Heartbeat);
                Message.addSenderUuid(builder, sender_uuid);
                Message.addWorldName(builder, worldName);

                int message = Message.endMessage(builder);
                builder.finish(message);

                byte[] buf = builder.sizedByteArray();
                pushSocket.send(buf, zmq.ZMQ.ZMQ_DONTWAIT);
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
