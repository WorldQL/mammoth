package com.worldql.client;

import WorldQLFB.StandardEvents.Update;
import com.google.protobuf.InvalidProtocolBufferException;
import com.worldql.client.compiled_protobuf.MinecraftPlayer;
import com.worldql.client.compiled_protobuf.WorldQLQuery;
import com.worldql.client.ghost.PlayerGhostManager;
import org.bukkit.plugin.Plugin;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class ZeroMQServer implements Runnable {
    private static Plugin plugin;
    private static String port;

    public ZeroMQServer(Plugin plugin, String port) {
        this.plugin = plugin;
        this.port = port;
    }

    @Override
    public void run() {
        try (ZContext context = new ZContext()) {
            ZMQ.Socket socket = context.createSocket(SocketType.PULL);
            socket.bind("tcp://*:" + port);

            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("Waiting for push...");
                byte[] reply = socket.recv(0);
                java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(reply);
                Update update = Update.getRootAsUpdate(buf);

                if (update.instruction().equals("MinecraftPlayerMove")) {
                    PlayerGhostManager.updateNPC(update);
                }

                /*
                try {

                    WorldQLQuery.WQL message = WorldQLQuery.WQL.parseFrom(reply);
                    if (message.hasPlayerState()) {
                        MinecraftPlayer.PlayerState state = message.getPlayerState();
                        PlayerGhostManager.updateNPC(state);
                    }


                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
            */


            }
            socket.close();
            context.destroy();
        }
    }


}
