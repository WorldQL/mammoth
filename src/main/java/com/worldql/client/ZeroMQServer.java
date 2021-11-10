package com.worldql.client;

import WorldQLFB.StandardEvents.Update;
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
    private final String port;
    private final ZContext context;

    public ZeroMQServer(Plugin plugin, String port, ZContext context) {
        this.plugin = plugin;
        this.port = port;
        this.context = context;
    }

    @Override
    public void run() {
        ZMQ.Socket socket = context.createSocket(SocketType.PULL);
        socket.bind("tcp://*:" + port);

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
