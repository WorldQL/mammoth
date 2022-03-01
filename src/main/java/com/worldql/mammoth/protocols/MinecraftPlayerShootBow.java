package com.worldql.mammoth.protocols;

import com.google.flatbuffers.FlexBuffers;
import com.worldql.mammoth.MammothPlugin;
import com.worldql.mammoth.worldql_serialization.Message;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.EntityPlayer;
import org.bukkit.Location;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class MinecraftPlayerShootBow {

    @SuppressWarnings("unchecked")
    public static void process(Message state, EntityPlayer entity) {
        FlexBuffers.Map playerMessageMap = FlexBuffers.getRoot(state.flex()).asMap();
        DataWatcher drawingBowData = new DataWatcher(entity);
        DataWatcherObject dwObject = new DataWatcherObject<>(8, DataWatcherRegistry.a);
        drawingBowData.a(dwObject, 0);

        byte handStateBitmask = 0;
        if (playerMessageMap.get("charging").asBoolean()) {
            if (playerMessageMap.get("offhand").asBoolean())
                handStateBitmask = 0b00000011;
            else
                handStateBitmask = 0x00000001;
        }

        drawingBowData.b(dwObject, handStateBitmask);

        ProtocolManager.sendGenericPacket(new PacketPlayOutEntityMetadata(entity.ae(),
                drawingBowData, false));

        // shoots the arrow from the npc.
        if (!playerMessageMap.get("charging").asBoolean()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Location loc = entity.getBukkitEntity().getLocation().add(0,1.4,0);
                    Vector v = entity.getBukkitEntity().getLocation().getDirection();

                    Arrow arrow = entity.getBukkitEntity().getWorld().spawnArrow(
                            loc, v, 1, 0);
                    arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                    arrow.setVelocity(v.normalize());
                }
            }.runTask(MammothPlugin.getPluginInstance());
        }
    }

}
