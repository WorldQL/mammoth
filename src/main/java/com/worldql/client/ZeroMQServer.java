package com.worldql.client;

import com.worldql.client.ghost.PlayerGhostManager;
import com.worldql.client.listeners.utils.BlockTools;
import com.worldql.client.serialization.Instruction;
import com.worldql.client.serialization.Message;
import com.worldql.client.serialization.Replication;
import org.bukkit.plugin.Plugin;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

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

        Message message = new Message(
                Instruction.Handshake,
                WorldQLClient.worldQLClientId,
                "@global",
                Replication.ExceptSelf,
                null,
                null,
                null,
                hostname + ":" + port,
                null
        );

        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.DONTWAIT);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] reply = socket.recv(0);
                java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(reply);
                var incoming = Message.decode(buf);

                if (incoming.instruction() == Instruction.Handshake) {
                    WorldQLClient.getPluginInstance().getLogger().info("Response from WorldQL handshake: " + incoming.parameter());
                    continue;
                }

                if (incoming.instruction() == Instruction.LocalMessage) {
                    if (incoming.parameter().equals("MinecraftBlockUpdate")) {
                        BlockTools.setRecords(incoming.records());
                        continue;
                    }
                    if (incoming.parameter().startsWith("MinecraftPlayer")) {
                        PlayerGhostManager.updateNPC(incoming);
                    }
                }


            } catch (ZMQException e) {
                if (e.getErrorCode() == ZMQ.Error.ETERM.getCode()) {
                    break;
                }
            }
        }
        socket.setLinger(0);
        socket.close();
    }


}
