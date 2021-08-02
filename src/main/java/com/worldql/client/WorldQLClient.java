package com.worldql.client;

import com.worldql.client.listeners.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public class WorldQLClient extends JavaPlugin {
    public static JavaPlugin plugin_instance;
    private static Thread ZeroMQThread;
    private static ZContext context;
    public static ZMQ.Socket push_socket;
    public static int zmqPortClientId;

    @Override
    public void onEnable() {
        plugin_instance = this;
        getLogger().info("Initializing Mammoth WorldQL client.");
        context = new ZContext();
        push_socket = context.createSocket(SocketType.PUSH);
        ZMQ.Socket handshake_socket = context.createSocket(SocketType.REQ);
        handshake_socket.connect("tcp://127.0.0.1:5556");


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
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.

            statement.executeUpdate("create table if not exists chunk_sync (pk INTEGER PRIMARY KEY, x integer, y integer, last_update integer);");
        } catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            System.err.println(e.getMessage());
        } finally {
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }

        handshake_socket.send(myIP.getBytes(ZMQ.CHARSET), 0);
        byte[] reply = handshake_socket.recv(0);
        String assignedZeroMQPort = new String(reply, ZMQ.CHARSET);
        zmqPortClientId = Integer.parseInt(assignedZeroMQPort);

        push_socket.connect("tcp://127.0.0.1:5555");
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

        this.getCommand("refreshworld").setExecutor(new TestRefreshWorldCommand());

        ZeroMQThread = new Thread(new ZeroMQServer(this, assignedZeroMQPort, context));
        ZeroMQThread.start();
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down ZeroMQ thread.");
        context.close();
        try {
            ZeroMQThread.interrupt();
            ZeroMQThread.join();
        } catch (InterruptedException e) {
        }
    }
}
