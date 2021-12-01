package com.worldql.client.protocols;

import com.google.flatbuffers.FlexBuffers;
import com.worldql.client.serialization.Message;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.EntityPlayer;

public class MinecraftPlayerShieldUse {

    @SuppressWarnings("unchecked")
    public static void process(Message state, EntityPlayer entity) {
        FlexBuffers.Map playerMessageMap = FlexBuffers.getRoot(state.flex()).asMap();
        DataWatcher holdingShieldData = new DataWatcher(entity);
        DataWatcherObject dwObject = new DataWatcherObject<>(8, DataWatcherRegistry.a);
        holdingShieldData.a(dwObject, 0);

        byte handStateBitmask = 0;
        if (playerMessageMap.get("blocking").asBoolean()) {
            if (playerMessageMap.get("offhand").asBoolean())
                handStateBitmask = 0b00000011;
            else
                handStateBitmask = 0x00000001;
        }

        holdingShieldData.b(dwObject, handStateBitmask);

        ProtocolManager.sendGenericPacket(new PacketPlayOutEntityMetadata(entity.ae(),
                holdingShieldData, false));
    }

}
