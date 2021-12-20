package com.worldql.client;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
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
import com.worldql.client.serialization.Instruction;
import com.worldql.client.serialization.Message;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
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
        getLogger().info("Initializing Mammoth WorldQL client v0.4-WorldGuard");
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


        // Initialize Protocol
        ProtocolManager.read();

        // For syncing player movements
        getServer().getPluginManager().registerEvents(new PlayerMoveAndLookHandler(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinEventListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerCrouchListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractEventListener(), this);
        getServer().getPluginManager().registerEvents(new ChunkLoadEventListener(), this);
        getServer().getPluginManager().registerEvents(new ChunkUnloadEventListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerHeldItemListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerArmorEditListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerShieldInteractListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerTeleportEventListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerLogOutListener(), this);
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

        try {
            // TODO: Remove and move to separate file so the plugin can still load without WorldGuard.
            World world = Bukkit.getWorld("world");
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(world));

            ProtectedRegion r = regions.getRegion("default_deny_area");
            if (r == null) {
                BlockVector3 min = BlockVector3.at(-2000, world.getMinHeight(), -2000);
                BlockVector3 max = BlockVector3.at(2000, world.getMaxHeight(), 2000);
                r = new ProtectedCuboidRegion("default_deny_area", min, max);
                regions.addRegion(r);
            }
            WorldQLClient.getPluginInstance().getLogger().info("Created default WorldGuard protection region.");

        } catch (Exception e) {
            System.out.println("Did not initialize default protected WorldGuard region.");
        }

        zeroMQThread = new Thread(new ZeroMQServer(this, context, selfHostname));
        zeroMQThread.start();
    }

    @Override
    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            PlayerLogOutListener.saveInventory(player);
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

    public int getZmqPortClientId() {
        return zmqPortClientId;
    }
}
