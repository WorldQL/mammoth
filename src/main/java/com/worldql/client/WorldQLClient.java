package com.worldql.client;

import com.worldql.client.listeners.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.net.DatagramSocket;
import java.net.InetAddress;


public class WorldQLClient extends JavaPlugin {
    public static WorldQLClient pluginInstance;
    private Thread zeroMQThread;
    private ZContext context;
    private ZMQ.Socket pushSocket;
    private int zmqPortClientId;
    private PacketReader packetReader;

    @Override
    public void onEnable() {
        pluginInstance = this;
        getLogger().info("Initializing Mammoth WorldQL client.");

        String selfHostname, worldqlHost;
        Integer worldqlPushPort, worldqlHandshakePort;


        if (System.getenv("WQL_HOST") == null){
            saveDefaultConfig();
            worldqlHost = getConfig().getString("worldql.host", "127.0.0.1");
            worldqlPushPort = getConfig().getInt("worldql.push-port", 5555);
            worldqlHandshakePort = getConfig().getInt("worldql.handshake-port", 5556);
        } else {
            worldqlHost = System.getenv("WQL_HOST").trim();
            worldqlPushPort = Integer.parseInt(System.getenv("WQL_PUSH_PORT").trim());
            worldqlHandshakePort = Integer.parseInt(System.getenv("WQL_HANDSHAKE_PORT").trim());
            if (System.getenv("WQL_PUSH_PORT") == null || System.getenv("WQL_HANDSHAKE_PORT") == null){
                getLogger().info("Please set 'WQL_PUSH_PORT' and 'WQL_PUSH_PORT' variables!");
                return;
            }
        }

        if (worldqlHost.equals("localhost") || worldqlHost.equals("127.0.0.1")) {
            selfHostname = "127.0.0.1";
        } else {
            try (final DatagramSocket datagramSocket = new DatagramSocket()) {
                datagramSocket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                selfHostname = datagramSocket.getLocalAddress().getHostAddress();
            } catch (Exception e) {
                throw new RuntimeException("Couldn't determine our IP address.");
            }
        }

        context = new ZContext();
        pushSocket = context.createSocket(SocketType.PUSH);
        packetReader = new PacketReader();
        ZMQ.Socket handshakeSocket = context.createSocket(SocketType.REQ);
        handshakeSocket.connect("tcp://%s:%d".formatted(worldqlHost, worldqlHandshakePort));


        handshakeSocket.send(selfHostname.getBytes(ZMQ.CHARSET), 0);
        byte[] reply = handshakeSocket.recv(0);
        String assignedZeroMQPort = new String(reply, ZMQ.CHARSET);
        zmqPortClientId = Integer.parseInt(assignedZeroMQPort);

        pushSocket.connect("tcp://%s:%d".formatted(worldqlHost, worldqlPushPort));

        getServer().getPluginManager().registerEvents(new PlayerMoveAndLookHandler(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinEventListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerCrouchListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractEventListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerLogOutListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerBlockPlaceListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerEditSignListener(), this);
        getServer().getPluginManager().registerEvents(new PortalCreateEventListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerLoadChunkListener(), this);
        getServer().getPluginManager().registerEvents(new OutgoingPlayerHitListener(), this);
        getServer().getPluginManager().registerEvents(new BlockDropItemEventListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerTeleportEventListener(), this);

        this.getCommand("refreshworld").setExecutor(new TestRefreshWorldCommand());

        zeroMQThread = new Thread(new ZeroMQServer(this, assignedZeroMQPort, context));
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
