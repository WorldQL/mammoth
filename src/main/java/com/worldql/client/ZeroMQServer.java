package com.worldql.client;

import com.google.protobuf.InvalidProtocolBufferException;
import com.worldql.client.compiled_protobuf.MinecraftPlayer;
import com.worldql.client.compiled_protobuf.WorldQLQuery;
import com.worldql.client.ghost.PacketNPCManager;
import org.bukkit.plugin.Plugin;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class ZeroMQServer implements Runnable {
    private static Plugin plugin;

    public ZeroMQServer(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try (ZContext context = new ZContext()) {
            ZMQ.Socket socket = context.createSocket(SocketType.PULL);
            socket.bind("tcp://*:29900");

            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("Waiting for push...");
                byte[] reply = socket.recv(0);

                try {
                    WorldQLQuery.WQL message = WorldQLQuery.WQL.parseFrom(reply);
                    if (message.hasPlayerState()) {
                        MinecraftPlayer.PlayerState state = message.getPlayerState();
                        PacketNPCManager.updateNPC(state);
                    }
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }


            }
            socket.close();
            context.destroy();
        }
    }

    /*
    new BukkitRunnable() {

                   @Override
                   public void run() {
                       // What you want to schedule goes here
                       plugin_instance.getServer().broadcastMessage(
                               "Welcome to Bukkit! Remember to read the documentation!");
                   }

               }.runTask(plugin_instance);

               try {
                   Thread.sleep(1000);
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
     */
}
