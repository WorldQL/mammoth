package com.worldql.client;

import com.worldql.client.listeners.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


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
        context = new ZContext();
        pushSocket = context.createSocket(SocketType.PUSH);
        packetReader = new PacketReader();
        ZMQ.Socket handshakeSocket = context.createSocket(SocketType.REQ);
        handshakeSocket.connect("tcp://127.0.0.1:5556");


        String myIP = "127.0.0.1";
        /*
        try (final DatagramSocket datagramSocket = new DatagramSocket()) {
            datagramSocket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            myIP = datagramSocket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            throw new RuntimeException("Couldn't determine our IP address.");
        }
         */

        Connection connection = null;
        try {
            // create a database connection
            connection = DriverManager.getConnection("jdbc:sqlite:worldql.db");
            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(30);  // set timeout to 30 sec.

                statement.executeUpdate("create table if not exists chunk_sync (pk INTEGER PRIMARY KEY, x integer, y integer, last_update integer);");
            }
        } catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            getLogger().severe(e.getMessage());
        }

        handshakeSocket.send(myIP.getBytes(ZMQ.CHARSET), 0);
        byte[] reply = handshakeSocket.recv(0);
        String assignedZeroMQPort = new String(reply, ZMQ.CHARSET);
        zmqPortClientId = Integer.parseInt(assignedZeroMQPort);

        pushSocket.connect("tcp://127.0.0.1:5555");

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
