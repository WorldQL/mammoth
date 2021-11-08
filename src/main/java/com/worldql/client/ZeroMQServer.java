package com.worldql.client;

import WorldQLFB_OLD.StandardEvents.Update;
import com.worldql.client.Messages.Message;
import com.worldql.client.Messages.Instruction;

import com.google.flatbuffers.FlatBufferBuilder;
import com.worldql.client.ghost.PlayerGhostManager;
import com.worldql.client.incoming.PlayerHit;
import com.worldql.client.incoming.ResponseRecordGetBlocksAll;
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
        System.out.println("BOUND TO PORT " + port);

        FlatBufferBuilder builder = new FlatBufferBuilder(1024);
        int parameter = builder.createString(hostname + ":" + port);
        int client_uuid = builder.createString(WorldQLClient.worldQLClientId);
        Message.startMessage(builder);
        Message.addInstruction(builder, Instruction.Handshake);
        Message.addParameter(builder, parameter);
        Message.addSenderUuid(builder, client_uuid);
        int message = Message.endMessage(builder);
        builder.finish(message);
        byte[] handshakeBuf = builder.sizedByteArray();
        WorldQLClient.getPluginInstance().getPushSocket().send(handshakeBuf, ZMQ.DONTWAIT);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] reply = socket.recv(0);
                java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(reply);
                Update update = Update.getRootAsUpdate(buf);

                if (update.instruction().equals("Response.Record.Get.Blocks.all")) {
                    ResponseRecordGetBlocksAll.process(update, this.plugin);
                }

                if (update.instruction().equals("MinecraftPlayerMove")) {
                    PlayerGhostManager.updateNPC(update);
                }
                if (update.instruction().equals("MinecraftPlayerQuit")) {
                    PlayerGhostManager.updateNPC(update);
                }
                if (update.instruction().equals("EntityHitEvent")) {
                    PlayerHit.process(update, this.plugin);
                }

                if (update.instruction().equals("NoRepeat.BlockBreak")) {
                    NoRepeatBlockBreak.spawnDrops(update);
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
