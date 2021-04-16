package com.worldql.client;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

import org.bukkit.plugin.java.JavaPlugin;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class WorldQLClient extends JavaPlugin {
    private ProtocolManager manager;
    private static JavaPlugin plugin_instance;
    private static Thread ZeroMQThread;
    public static ZMQ.Socket push_socket;

    @Override
    public void onEnable() {
        manager = ProtocolLibrary.getProtocolManager();
        plugin_instance = this;


        getLogger().info("Initializing Mammoth WorldQL client.");
        this.getCommand("spawnghost").setExecutor(new SpawnCommandHandler(manager));
        this.getCommand("stepghost").setExecutor(new StepCommandHandler(manager, this));

        ZContext context = new ZContext();
        push_socket = context.createSocket(SocketType.PUSH);


        ZMQ.Socket handshake_socket = context.createSocket(SocketType.REQ);
        handshake_socket.connect("tcp://127.0.0.1:5556");

        String myIP;

        try(final DatagramSocket datagramSocket = new DatagramSocket()){
            datagramSocket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            myIP = datagramSocket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            throw new RuntimeException("Couldn't determine our IP address.");
        }

        handshake_socket.send(myIP.getBytes(ZMQ.CHARSET), 0);
        byte[] reply = handshake_socket.recv(0);
        String assignedZeroMQPort = new String(reply, ZMQ.CHARSET);

        push_socket.connect("tcp://127.0.0.1:5555");
        getServer().getPluginManager().registerEvents(new PlayerMoveAndLookHandler(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinEventListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerCrouchListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractEventListener(), this);
        ZeroMQThread = new Thread(new ZeroMQServer(this, assignedZeroMQPort));
        ZeroMQThread.start();
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down ZeroMQ thread.");
        ZeroMQThread.interrupt();
    }
}
