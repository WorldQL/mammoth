package com.worldql.client.protocols;

import com.google.flatbuffers.FlexBuffers;
import com.worldql.client.serialization.Message;
import net.minecraft.network.protocol.game.PacketPlayOutAnimation;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.EntityPose;

public class MinecraftPlayerSingleAction {

    public static void process(Message state, EntityPlayer entity) {
        FlexBuffers.Map playerMessageMap = FlexBuffers.getRoot(state.flex()).asMap();

        String action = playerMessageMap.get("action").asString();
        DataWatcher dw = new DataWatcher(null);
        if (action.equals("crouch")) {
            dw.a(new DataWatcherObject<>(6, DataWatcherRegistry.s), EntityPose.f);
            PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(entity.ae(), dw, true);
            ProtocolManager.sendGenericPacket(packet);
        }
        if (action.equals("uncrouch")) {
            dw.a(new DataWatcherObject<>(6, DataWatcherRegistry.s), EntityPose.a);
            PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(entity.ae(), dw, true);
            ProtocolManager.sendGenericPacket(packet);
        }
        if (action.equals("punch")) {
            PacketPlayOutAnimation punch = new PacketPlayOutAnimation(entity, (byte) 0);
            ProtocolManager.sendGenericPacket(punch);
        }
    }

}
