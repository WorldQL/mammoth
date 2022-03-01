package com.worldql.mammoth;

import com.google.flatbuffers.FlexBuffers;
import com.worldql.mammoth.commands.CommandTeleportRequest;
import com.worldql.mammoth.commands.CommandTeleportTo;
import com.worldql.mammoth.ghost.PlayerGhostManager;
import com.worldql.mammoth.listeners.player.PlayerChatListener;
import com.worldql.mammoth.listeners.player.PlayerDeathListener;
import com.worldql.mammoth.listeners.utils.BlockTools;
import com.worldql.mammoth.minecraft_serialization.VillagerTransfer;
import com.worldql.mammoth.worldql_serialization.Instruction;
import com.worldql.mammoth.worldql_serialization.Message;
import com.worldql.mammoth.worldql_serialization.Replication;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.time.Instant;

public class ZeroMQServer implements Runnable {
    private final Plugin plugin;
    private final ZContext context;
    private final String hostname;

    public ZeroMQServer(Plugin plugin, ZContext context, String hostname) {
        this.plugin = plugin;
        this.context = context;
        this.hostname = hostname;
    }

    @Override
    public void run() {
        ZMQ.Socket socket = context.createSocket(SocketType.PULL);
        int port = socket.bindToRandomPort("tcp://" + hostname, 29000, 30000);
        MammothPlugin.zeroMQServerPort = port;

        Message message = new Message(
                Instruction.Handshake,
                MammothPlugin.worldQLClientId,
                "@global",
                Replication.ExceptSelf,
                null,
                null,
                null,
                hostname + ":" + port,
                null
        );

        MammothPlugin.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.DONTWAIT);

        while (true) {
            try {
                byte[] reply = socket.recv(0);
                java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(reply);
                var incoming = Message.decode(buf);
                boolean isSelf = incoming.senderUuid().equals(MammothPlugin.worldQLClientId);

                if (incoming.instruction() == Instruction.Handshake) {
                    MammothPlugin.getPluginInstance().getLogger().info("Got successful handshake response from WorldQL server!");
                    continue;
                }

                if (incoming.instruction() == Instruction.Heartbeat) {
                    //WorldQLClient.getPluginInstance().getLogger().info("Heartbeat from WorldQL");
                    MammothPlugin.timestampOfLastHeartbeat = Instant.now().toEpochMilli();
                    continue;
                }

                if (incoming.instruction() == Instruction.GlobalMessage) {
                    if (incoming.parameter().equals("MinecraftNightSkip")) {
                        Bukkit.getScheduler().runTask(MammothPlugin.getPluginInstance(), () -> {
                           Bukkit.getWorld(MammothPlugin.worldName).setTime(0);
                        });
                    }
                    if (incoming.parameter().startsWith("MinecraftRPC")) {
                        String[] input = incoming.parameter().split(">", 2);
                        String command = input[1];
                        Bukkit.getScheduler().runTask(MammothPlugin.getPluginInstance(), () -> {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                        });
                    }

                    if (incoming.parameter().equals("MinecraftPlayerChat")) {
                        PlayerChatListener.relayChat(incoming);
                    }

                    if (incoming.parameter().equals("MinecraftPlayerDeath")) {
                        PlayerDeathListener.handleIncomingDeath(incoming, isSelf);
                    }

                    if (incoming.parameter().equals("MinecraftBlockUpdate")) {
                        if (isSelf) {
                            continue;
                        }
                        BlockTools.setRecords(incoming.records(), false);
                        continue;
                    }

                    if (incoming.parameter().equals("MinecraftTeleportRequest")) {
                        CommandTeleportRequest.handlePositionLookup(incoming);
                    }
                    if (incoming.parameter().equals("MinecraftTeleportPositionLookup")) {
                        CommandTeleportTo.handlePositionLookup(incoming);
                    }

                    if (incoming.parameter().equals("MinecraftTeleport")) {
                        CommandTeleportRequest.handleTeleport(incoming);
                    }

                    if (incoming.parameter().equals("MinecraftVillagerTransfer")) {
                        VillagerTransfer.handleIncomingVillager(incoming);
                    }
                }

                if (incoming.instruction() == Instruction.LocalMessage) {
                    if (incoming.parameter().equals("MinecraftBlockUpdate")) {
                        BlockTools.setRecords(incoming.records(), isSelf);
                        continue;
                    }
                    if (incoming.parameter().equals("MinecraftEndCrystalCreate")) {
                        BlockTools.createEndCrystal(incoming.position(), incoming.worldName());
                        continue;
                    }
                    if (incoming.parameter().startsWith("MinecraftPlayer")) {
                        PlayerGhostManager.updateNPC(incoming);
                    }
                    if (incoming.parameter().equals("MinecraftExplosion")) {
                        float radius = (float) FlexBuffers.getRoot(incoming.flex()).asMap().get("radius").asFloat();
                        BlockTools.createExplosion(incoming.position(), incoming.worldName(), radius);
                    }
                    if (incoming.parameter().equals("MinecraftPrimeTNT")) {
                        BlockTools.createPrimedTNT(incoming.position(), incoming.worldName());
                    }
                }

                if (incoming.instruction() == Instruction.RecordReply) {
                    if (!incoming.records().isEmpty()) {
                        BlockTools.setRecords(incoming.records(), false);
                    }
                }

            } catch (Exception e) {
                if (e instanceof ZMQException) {
                    if (((ZMQException) e).getErrorCode() == ZMQ.Error.ETERM.getCode()) {
                        MammothPlugin.getPluginInstance().getLogger().info("Caught ZeroMQ ETERM exception!");
                        break;
                    }
                }
            }
        }
        socket.setLinger(0);
        socket.close();
    }


}
