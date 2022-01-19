package com.worldql.client.minecraft_serialization;

import com.google.flatbuffers.FlexBuffers;
import com.google.flatbuffers.FlexBuffersBuilder;
import com.worldql.client.WorldQLClient;
import com.worldql.client.worldql_serialization.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import zmq.ZMQ;

import java.nio.ByteBuffer;
import java.util.UUID;

public class VillagerTransfer {
    public static void sendVillagerTransferMessage(Player p, String villagerNBT) {
        FlexBuffersBuilder b = Codec.getFlexBuilder();
        int pmap = b.startMap();
        b.putString("villagernbt", villagerNBT);
        b.putString("uuid", p.getUniqueId().toString());
        b.endMap(null, pmap);
        ByteBuffer bb = b.finish();

        Message message = new Message(
                Instruction.GlobalMessage,
                WorldQLClient.worldQLClientId,
                "@global",
                Replication.ExceptSelf,
                new Vec3D(p.getLocation()),
                null,
                null,
                "MinecraftVillagerTransfer",
                bb
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);
    }
    public static void handleIncomingVillager(Message incoming) {
        FlexBuffers.Map villagerMessageMap = FlexBuffers.getRoot(incoming.flex()).asMap();
        String nbt = villagerMessageMap.get("villagernbt").asString();
        Bukkit.getScheduler().runTask(WorldQLClient.getPluginInstance(), () -> {
            Player p = Bukkit.getPlayer(UUID.fromString(villagerMessageMap.get("uuid").asString()));
            if (p != null) {
                Villager v = (Villager) p.getWorld().spawnEntity(p.getLocation(), EntityType.VILLAGER);
                SaveLoadPlayerFromRedis.setNBT(v, nbt);
                Bukkit.getScheduler().runTaskLater(WorldQLClient.getPluginInstance(), () -> {
                    v.teleport(p);
                    if (p.isInsideVehicle()) {
                        p.getVehicle().addPassenger(v);
                    }
                }, 5L);
            }
        });
    }
}
