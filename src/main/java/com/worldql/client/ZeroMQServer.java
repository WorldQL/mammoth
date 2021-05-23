package com.worldql.client;

import WorldQLFB.StandardEvents.Update;
import com.worldql.client.ghost.PlayerGhostManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
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
                byte[] reply = socket.recv(0);
                java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(reply);
                Update update = Update.getRootAsUpdate(buf);
                System.out.println(update.instruction());


                if (update.instruction().equals("Response.Record.Get.Blocks.all")) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            // What you want to schedule goes here
                            World world = Bukkit.getWorld("world");
                            for (int i = 0; i < update.paramsLength(); i++) {
                                String block_data = update.params(i);
                                System.out.println(block_data);
                                double blockx = update.numericalParams(i * 3);
                                double blocky = update.numericalParams(i * 3 + 1);
                                double blockz = update.numericalParams(i * 3 + 2);
                                String[] block_datas = block_data.split("\n");
                                System.out.println(block_datas.length);
                                BlockData blockData = Bukkit.getServer().createBlockData(block_datas[0]);
                                Location l = new Location(world, blockx, blocky, blockz);
                                if (Tag.BEDS.isTagged(blockData.getMaterial())) {
                                    Bed bed = (Bed) blockData;
                                    l = l.add(bed.getFacing().getDirection());
                                    DirectionalUtilities.setBed(world.getBlockAt(l), bed.getFacing(), blockData.getMaterial());
                                } else {
                                   world.getBlockAt(l).setBlockData(blockData);
                                }


                                if (block_datas.length > 1) {
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            System.out.println("SIGN");
                                            Sign sign = (Sign) world.getBlockAt(new Location(world, blockx, blocky, blockz)).getState();
                                            for (int j = 1; j < block_datas.length; j++){
                                                sign.setLine(j-1, block_datas[j]);
                                            }
                                            sign.update();
                                        }
                                    }.runTaskLater(plugin, 2);
                                }
                            }
                        }

                    }.runTask(this.plugin);
                }

                if (update.instruction().equals("MinecraftPlayerMove")) {
                    PlayerGhostManager.updateNPC(update);
                }
                if (update.instruction().equals("MinecraftPlayerQuit")) {
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
