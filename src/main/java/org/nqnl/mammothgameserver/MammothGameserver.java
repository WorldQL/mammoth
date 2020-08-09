package org.nqnl.mammothgameserver;

import io.lettuce.core.RedisClient;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.nqnl.mammothgameserver.commands.RemoveBed;
import org.nqnl.mammothgameserver.events.RemoteBlockChangeEvent;
import org.nqnl.mammothgameserver.listeners.*;
import org.nqnl.mammothgameserver.util.ServerTransferPayload;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class MammothGameserver extends JavaPlugin {
    public static JedisPool pool;
    private static MammothGameserver instance;

    @Override
    public void onEnable() {
        pool = new JedisPool("localhost", 6379);
        this.instance = this;
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getPluginManager().registerEvents(new PlayerMoveEventListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerPortalEventListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInventoryEventListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerBlockBreakPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitEventListener(), this);
        getServer().getPluginManager().registerEvents(new BlockRedstoneEventListener(), this);
        getServer().getPluginManager().registerEvents(new EntityExplodeEventLister(), this);
        getServer().getPluginManager().registerEvents(new RemoteBlockChangeEventListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerBedEnterEventListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerRespawnEventListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinEventListener(), this);

        this.getCommand("removebed").setExecutor(new RemoveBed());

        RedisClient client = RedisClient.create("redis://localhost:6379/");
        StatefulRedisPubSubConnection<String, String> con = client.connectPubSub();
        RedisPubSubListener<String, String> listener = new RedisPubSubAdapter<String, String>() {
            @Override
            public void message(String channel, String message) {
                RemoteBlockChangeEvent r = new RemoteBlockChangeEvent(message);
                Bukkit.getScheduler().runTask(instance, () -> Bukkit.getPluginManager().callEvent(r));
            }
        };
        con.addListener(listener);
        RedisPubSubCommands<String, String> sync = con.sync();
        sync.subscribe("blockevents");

        getLogger().info("Mammoth Gameserver enabled!");
    }
    @Override
    public void onDisable() {
        getLogger().info("onDisable is called!");
        Jedis j = pool.getResource();
        try {
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                String playerData = ServerTransferPayload.createPayload(player);
                j.set("player-"+player.getUniqueId(), playerData);
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            j.close();
        }
    }
}
