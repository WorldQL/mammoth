package com.worldql.mammoth;

import com.worldql.mammoth.ghost.ExpiringEntityPlayer;
import com.worldql.mammoth.ghost.PlayerGhostManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutAnimation;
import net.minecraft.server.network.PlayerConnection;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.List;

public class PacketReader {
    //public final Map<UUID, Channel> channels;

    public PacketReader() {
        //this.channels = new HashMap<>();
    }

    private void inject(Player player) {
        CraftPlayer craftPlayer = (CraftPlayer) player;
        Channel channel = craftPlayer.getHandle().b.a.k;
        //channels.put(player.getUniqueId(), channel);

        if (channel.pipeline().get("PacketInjector") != null) {
            return;
        }

        channel.pipeline().addAfter("decoder", "PacketInjector", new MessageToMessageDecoder<Packet<?>>() {
            @Override
            protected void decode(ChannelHandlerContext channel, Packet<?> packet, List<Object> arg) throws Exception {
                arg.add(packet);
                readPacket(player, packet);
            }

        });
    }

    public void readPacket(Player player, Packet<?> packet) {
        if (packet.getClass().getSimpleName().equalsIgnoreCase("PacketPlayInUseEntity")) {
            if (getValue(packet,
                    "b").getClass().getName().equals("net.minecraft.network.protocol.game.PacketPlayInUseEntity$1")) {
                int playerId = (int) getValue(packet, "a");
                ExpiringEntityPlayer p = PlayerGhostManager.integerNPCLookup.get(playerId);

                PlayerConnection connection = ((CraftPlayer) player).getHandle().b;


                if (p == null) {
                    return;
                }

            //    Bukkit.getScheduler().runTask(WorldQLClient.getPluginInstance(),
             //           () -> Bukkit.getPluginManager().callEvent(new OutgoingPlayerHitEvent(playerId, player.getLocation().getDirection())));

                PacketPlayOutAnimation damage = new PacketPlayOutAnimation(p.grab(), (byte) 1);
                connection.a(damage);
            }
            /*
            if (getValue(packet, "b").getClass().getName() == "net.minecraft.network.protocol.game.PacketPlayInUseEntity$d") {
                WorldQLClient.logger.info("RIGHT CLICK");
            }

             */

        }
    }

    private Object getValue(Object instance, String name) {
        Object result = null;

        try {
            Field field = instance.getClass().getDeclaredField(name);
            field.setAccessible(true);
            result = field.get(instance);
            field.setAccessible(false);
        } catch (Exception ignored) {

        }

        return result;

    }

}
