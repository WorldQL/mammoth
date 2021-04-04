package com.worldql.client;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;


public class WorldQLClient extends JavaPlugin {
    private ProtocolManager manager;
    private static JavaPlugin plugin_instance;
    private static Thread ZeroMQThread;

    @Override
    public void onEnable() {
        manager = ProtocolLibrary.getProtocolManager();
        plugin_instance = this;


        getLogger().info("Initializing Mammoth WorldQL client.");
        this.getCommand("spawnghost").setExecutor(new SpawnCommandHandler(manager));
        this.getCommand("stepghost").setExecutor(new StepCommandHandler(manager, this));

        ZContext context = new ZContext();
        ZMQ.Socket socket = context.createSocket(SocketType.PUSH);
        socket.connect("tcp://127.0.0.1:5555");
        getServer().getPluginManager().registerEvents(new PlayerMoveAndLookHandler(socket), this);
        ZeroMQThread = new Thread(new ZeroMQServer(this));
        ZeroMQThread.start();
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down ZeroMQ thread.");
        ZeroMQThread.interrupt();
    }
}
