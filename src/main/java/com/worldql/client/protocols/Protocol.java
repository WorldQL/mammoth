package com.worldql.client.protocols;

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.mojang.authlib.GameProfile;
import io.netty.channel.*;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.login.PacketLoginInStart;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.server.network.ServerConnection;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_18_R1.CraftServer;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public abstract class Protocol {
    private static final AtomicInteger ID = new AtomicInteger(0);

    private final ServerConnection serverConnection;


    // Speedup channel lookup
    private final Map<String, Channel> channelLookup = new MapMaker().weakValues().makeMap();
    private Listener listener;

    // Channels that have already been removed
    private final Set<Channel> uninjectedChannels = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());

    // List of network markers
    private List<NetworkManager> networkManagers;

    // Injected channel handlers
    private final List<Channel> serverChannels = Lists.newArrayList();
    private ChannelInboundHandlerAdapter serverChannelHandler;
    private ChannelInitializer<Channel> beginInitProtocol;
    private ChannelInitializer<Channel> endInitProtocol;

    // Current handler name
    private final String handlerName;

    protected volatile boolean closed;
    protected Plugin plugin;

    public Protocol(Plugin plugin) {
        this.plugin = plugin;

        handlerName = getHandlerName();
        registerBukkitEvents();

        serverConnection = ((CraftServer) Bukkit.getServer()).getServer().ad();

        try {
            registerChannelHandler();
            registerPlayers(plugin);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().info("[WorldQL Protocol] Delaying server channel injection due to late bind.");

            new BukkitRunnable() {
                @Override
                public void run() {
                    registerChannelHandler();
                    registerPlayers(plugin);
                    plugin.getLogger().info("[WorldQL Protocol] Late bind injection successful.");
                }
            }.runTask(plugin);
        }
    }

    private void createServerChannelHandler() {
        endInitProtocol = new ChannelInitializer<>() {

            @Override
            protected void initChannel(Channel channel) throws Exception {
                try {
                    synchronized (networkManagers) {
                        if (!closed) {
                            channel.eventLoop().submit(() -> injectChannelInternal(channel));
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Cannot inject incoming channel " + channel, e);
                }
            }

        };

        // This is executed before Minecraft's channel handler
        beginInitProtocol = new ChannelInitializer<>() {

            @Override
            protected void initChannel(Channel channel) {
                channel.pipeline().addLast(endInitProtocol);
            }

        };

        serverChannelHandler = new ChannelInboundHandlerAdapter() {

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                Channel channel = (Channel) msg;

                channel.pipeline().addFirst(beginInitProtocol);
                ctx.fireChannelRead(msg);
            }

        };
    }

    private void registerBukkitEvents() {
        listener = new Listener() {

            @EventHandler(priority = EventPriority.LOWEST)
            public void onPlayerLogin(PlayerLoginEvent e) {
                if (closed)
                    return;

                Channel channel = getChannel(e.getPlayer());

                if (!uninjectedChannels.contains(channel)) {
                    injectPlayer(e.getPlayer());
                }
            }

            @EventHandler
            public void onPluginDisable(PluginDisableEvent e) {
                if (e.getPlugin().equals(plugin)) {
                    close();
                }
            }

        };

        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    public Object getValue(Object instance, String name) {
        Object result = null;
        try {
            Field field = instance.getClass().getDeclaredField(name);
            field.setAccessible(true);
            result = field.get(instance);
            field.setAccessible(false);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void registerChannelHandler() {
        networkManagers = (List<NetworkManager>) getValue(serverConnection, "g");
        createServerChannelHandler();

        for (ChannelFuture item :  (List<ChannelFuture>) getValue(serverConnection, "f")) {
            Channel serverChannel = (item).channel();

            serverChannels.add(serverChannel);
            serverChannel.pipeline().addFirst(serverChannelHandler);
        }
    }

    private void unregisterChannelHandler() {
        if (serverChannelHandler == null)
            return;

        for (Channel serverChannel : serverChannels) {
            ChannelPipeline pipeline = serverChannel.pipeline();
            serverChannel.eventLoop().execute(() -> {
                try {
                    pipeline.remove(serverChannelHandler);
                } catch (NoSuchElementException e) {
                    // ignore
                }
            });
        }
    }

    private void registerPlayers(Plugin plugin) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            injectPlayer(player);
        }
    }

    /**
     * Invoked when the server is starting to send a packet to a player.
     * Note that this is not executed on the main thread.
     *
     * @param receiver - the receiving player, NULL for early login/status packets.
     * @param channel - the channel that received the packet. Never NULL.
     * @param packet - the packet being sent.
     * @return The packet to send instead, or NULL to cancel the transmission.
     */
    public Object onPacketOutAsync(Player receiver, Channel channel, Object packet) {
        return packet;
    }

    /**
     * Invoked when the server has received a packet from a given player.
     *
     * @param sender - the player that sent the packet, NULL for early login/status packets.
     * @param channel - channel that received the packet. Never NULL.
     * @param packet - the packet being received.
     * @return The packet to receive instead, or NULL to cancel.
     */
    public Object onPacketInAsync(Player sender, Channel channel, Object packet) {
        return packet;
    }


    public void sendPacket(Channel channel, Object packet) {
        channel.pipeline().writeAndFlush(packet);
    }

    /**
     * Retrieve the name of the channel injector
     * <p>
     * Note that this method will only be invoked once. It is no longer necessary to override this to support multiple instances.
     *
     * @return A unique channel handler name.
     */
    protected String getHandlerName() {
        return plugin.getName() + "-" + ID.incrementAndGet();
    }

    /**
     * Add a custom channel handler to the given player's channel pipeline, allowing us to intercept sent and received packets.
     * <p>
     * This will automatically be called when a player has logged in.
     *
     * @param player - the player to inject.
     */
    public void injectPlayer(Player player) {
        injectChannelInternal(getChannel(player)).player = player;
    }

    /**
     * Add a custom channel handler to the given channel.
     *
     * @param channel - the channel to inject.
     * @return The intercepted channel, or NULL if it has already been injected.
     */
    public void injectChannel(Channel channel) {
        injectChannelInternal(channel);
    }

    /**
     * Add a custom channel handler to the given channel.
     *
     * @param channel - the channel to inject.
     * @return The packet interceptor.
     */
    private PacketInterceptor injectChannelInternal(Channel channel) {
        try {
            PacketInterceptor interceptor = (PacketInterceptor) channel.pipeline().get(handlerName);

            // Inject our packet interceptor
            if (interceptor == null) {
                interceptor = new PacketInterceptor();
                channel.pipeline().addBefore("packet_handler", handlerName, interceptor);
                uninjectedChannels.remove(channel);
            }

            return interceptor;
        } catch (IllegalArgumentException e) {
            // Try again
            return (PacketInterceptor) channel.pipeline().get(handlerName);
        }
    }

    /**
     * Retrieve the Netty channel associated with a player. This is cached.
     *
     * @param player - the player.
     * @return The Netty channel.
     */
    public Channel getChannel(Player player) {
        Channel channel = channelLookup.get(player.getName());

        if (channel == null) {
            PlayerConnection connection = ((CraftPlayer)player).getHandle().b;
            NetworkManager manager = connection.a;

            channelLookup.put(player.getName(), channel = manager.k);
        }

        return channel;
    }

    /**
     * Uninject a specific player.
     *
     * @param player - the injected player.
     */
    public void uninjectPlayer(Player player) {
        uninjectChannel(getChannel(player));
    }

    /**
     * Uninject a specific channel.
     * <p>
     * This will also disable the automatic channel injection that occurs when a player has properly logged in.
     *
     * @param channel - the injected channel.
     */
    public void uninjectChannel(Channel channel) {
        if (!closed) {
            uninjectedChannels.add(channel);
        }

        channel.pipeline().remove(handlerName);
       // channel.eventLoop().execute(() -> channel.pipeline().remove(handlerName));
    }

    public boolean hasInjected(Player player) {
        return hasInjected(getChannel(player));
    }

    public boolean hasInjected(Channel channel) {
        return channel.pipeline().get(handlerName) != null;
    }

    /**
     * Cease listening for packets. This is called automatically when your plugin is disabled.
     */
    public final void close() {
        if (!closed) {
            closed = true;

            // Remove our handlers
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                uninjectPlayer(player);
            }

            // Clean up Bukkit
            HandlerList.unregisterAll(listener);
            unregisterChannelHandler();
        }
    }

    /**
     * Channel handler that is inserted into the player's channel pipeline, allowing us to intercept sent and received packets.
     */
    private final class PacketInterceptor extends ChannelDuplexHandler {
        // Updated by the login event
        public volatile Player player;

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // Intercept channel
            Channel channel = ctx.channel();
            handleLoginStart(channel, msg);

            try {
                msg = onPacketInAsync(player, channel, msg);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error in onPacketInAsync().", e);
            }

            if (msg != null) {
                super.channelRead(ctx, msg);
            }
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            try {
                msg = onPacketOutAsync(player, ctx.channel(), msg);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error in onPacketOutAsync().", e);
            }

            if (msg != null) {
                super.write(ctx, msg, promise);
            }
        }

        private void handleLoginStart(Channel channel, Object packet) {
            if (packet instanceof PacketLoginInStart) {
                GameProfile profile = ((PacketLoginInStart)packet).b();
                channelLookup.put(profile.getName(), channel);
            }
        }
    }
}
