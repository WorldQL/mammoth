package com.worldql.client.protocols;

import com.google.flatbuffers.FlexBuffers;
import com.worldql.client.worldql_serialization.Message;
import net.minecraft.network.protocol.game.PacketPlayOutEntityHeadRotation;
import net.minecraft.network.protocol.game.PacketPlayOutEntityTeleport;
import net.minecraft.server.level.EntityPlayer;

public class MinecraftPlayerMove {
    
    public static void process(Message state, EntityPlayer entity) {
        FlexBuffers.Map playerMessageMap = FlexBuffers.getRoot(state.flex()).asMap();
        float playerYaw = (float) playerMessageMap.get("yaw").asFloat();
        entity.a(
                state.position().x(),
                state.position().y(),
                state.position().z(),
                playerYaw,
                (float) playerMessageMap.get("pitch").asFloat()
        );
        ProtocolManager.sendGenericPacket(new PacketPlayOutEntityTeleport(entity));
        ProtocolManager.sendGenericPacket(new PacketPlayOutEntityHeadRotation(entity, (byte) ((playerYaw * 256) / 360)));
    }
}
