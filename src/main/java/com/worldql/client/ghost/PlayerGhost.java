// credit to DanielTheDev for this 1.17.1 implementation.
// this file is unused

package com.worldql.client.ghost;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.datafixers.util.Pair;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.network.syncher.DataWatcherSerializer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.animal.EntityParrot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardTeam;
import net.minecraft.world.scores.ScoreboardTeamBase;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftParrot;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_17_R1.util.CraftChatMessage;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PlayerGhost {

    private static AtomicInteger atomicInteger;
    private final String hideTeam;
    private final int entityID; //unique entityID the server holds to find/modify existing entities. Be careful when assigning values that they do not overlap
    private GameProfile profile;
    private final NPCMetaData metadata = new NPCMetaData();
    private final Location location;
    private Ping ping = Ping.FIVE_BARS;
    private Gamemode gamemode = Gamemode.CREATIVE;
    private String displayName;

    static {
        try {
            Field field = Entity.class.getDeclaredField("b");
            field.setAccessible(true);
            atomicInteger = (AtomicInteger) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public PlayerGhost(UUID uuid, Location location, String displayName) {
        this.entityID = atomicInteger.incrementAndGet();

        this.profile = new GameProfile(uuid, displayName);
        this.location = location;
        this.displayName = displayName;
        this.hideTeam = "hide-" + Integer.toHexString(ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE));
    }

    public PlayerGhost(Location location, String displayName) {
        this(UUID.randomUUID(), location, displayName);
    }

    public void spawnNPC(Collection<Player> players) {
        players.forEach(this::spawnNPC);
    }

    public void spawnNPC(Player player) {
        this.addToTabList(player);
        this.sendPacket(player, this.getEntitySpawnPacket());
        this.updateMetadata(player);
    }

    public void destroyNPC(Collection<Player> players) {
        players.forEach(this::destroyNPC);
    }

    public void destroyNPC(Player player) {
        this.sendPacket(player, this.getPlayerInfoPacket(PlayerInfo.REMOVE_PLAYER));
        this.sendPacket(player, this.getEntityDestroyPacket());
    }

    public void reloadNPC(Collection<Player> players) {
        players.forEach(this::reloadNPC);
    }

    public void reloadNPC(Player player) {
        this.destroyNPC(player);
        this.spawnNPC(player);
    }

    public void teleportNPC(Collection<Player> players, Location location, boolean onGround) {
        players.forEach(p -> this.teleportNPC(p, location, onGround));
    }

    public void teleportNPC(Player player, Location location, boolean onGround) {
        this.location.setX(location.getX());
        this.location.setY(location.getY());
        this.location.setZ(location.getZ());
        this.location.setPitch(location.getPitch());
        this.location.setYaw(location.getYaw());
        this.sendPacket(player, this.getEntityTeleportPacket(onGround));
    }

    public void updateMetadata(Collection<Player> players) {
        players.forEach(this::updateMetadata);
    }

    public void updateMetadata(Player player) {
        this.sendPacket(player, this.getEntityMetadataPacket());
    }

    public void updateGameMode(Collection<Player> players) {
        players.forEach(this::updateGameMode);
    }

    public void updateGameMode(Player player) {
        this.sendPacket(player, this.getPlayerInfoPacket(PlayerInfo.UPDATE_GAME_MODE));
    }

    public void updatePing(Collection<Player> players) {
        players.forEach(this::updatePing);
    }

    public void updatePing(Player player) {
        this.sendPacket(player, this.getPlayerInfoPacket(PlayerInfo.UPDATE_LATENCY));
    }

    public void updateTabListName(Collection<Player> players) {
        players.forEach(this::updateTabListName);
    }

    public void updateTabListName(Player player) {
        this.sendPacket(player, this.getPlayerInfoPacket(PlayerInfo.UPDATE_DISPLAY_NAME));
    }

    public void removeFromTabList(Collection<Player> players) {
        players.forEach(this::removeFromTabList);
    }

    public void removeFromTabList(Player player) {
        this.sendPacket(player, this.getPlayerInfoPacket(PlayerInfo.REMOVE_PLAYER));
    }

    public void addToTabList(Collection<Player> players) {
        players.forEach(this::addToTabList);
    }

    public void addToTabList(Player player) {
        this.sendPacket(player, this.getPlayerInfoPacket(PlayerInfo.ADD_PLAYER));
    }

    public void playAnimation(Collection<Player> players, Animation animation) {
        players.forEach(p -> this.playAnimation(p, animation));
    }

    public void playAnimation(Player player, Animation animation) {
        this.sendPacket(player, this.getEntityAnimationPacket(animation));
    }

    public void rotateHead(Collection<Player> players, float pitch, float yaw) {
        players.forEach(p -> this.rotateHead(p, pitch, yaw));
    }

    public void rotateHead(Player player, float pitch, float yaw) {
        this.location.setPitch(pitch);
        this.location.setYaw(yaw);
        this.sendPacket(player, this.getEntityLookPacket());
        this.sendPacket(player, this.getEntityHeadRotatePacket());
    }

    public void setTabListName(String name) {
        this.displayName = name;
    }

    public void setEquipment(Collection<Player> players, ItemSlot slot, org.bukkit.inventory.ItemStack itemStack) {
        players.forEach(p -> this.setEquipment(p, slot, itemStack));
    }

    public void setEquipment(Player player, ItemSlot slot, org.bukkit.inventory.ItemStack itemStack) {
        this.sendPacket(player, this.getEntityEquipmentPacket(slot.getSlot(), CraftItemStack.asNMSCopy(itemStack)));
    }

    public void setPassenger(Collection<Player> players, int... entityIDs) {
        players.forEach(p -> this.setPassenger(p, entityIDs));
    }

    public void setPassenger(Player player, int... entityIDs) {
        this.sendPacket(player, getEntityAttachPacket(entityIDs));
    }

    private void sendPacket(Player player, Packet<?> packet) {
        ((CraftPlayer) (player)).getHandle().b.sendPacket(packet);
    }

    public void setNameTagVisibility(Collection<Player> players, boolean show) {
        players.forEach(p -> this.setNameTagVisibility(p, show));
    }

    public void setNameTagVisibility(Player player, boolean show) {
        ScoreboardTeam team = new ScoreboardTeam(new Scoreboard(), this.hideTeam);
        if (show) {
            PacketPlayOutScoreboardTeam leavePacket = PacketPlayOutScoreboardTeam.a(team, this.profile.getName(), PacketPlayOutScoreboardTeam.a.b);
            this.sendPacket(player, leavePacket);
        } else {
            team.setNameTagVisibility(ScoreboardTeamBase.EnumNameTagVisibility.b);
            PacketPlayOutScoreboardTeam createPacket = PacketPlayOutScoreboardTeam.a(team, true);
            PacketPlayOutScoreboardTeam joinPacket = PacketPlayOutScoreboardTeam.a(team, this.profile.getName(), PacketPlayOutScoreboardTeam.a.a);
            this.sendPacket(player, createPacket);
            this.sendPacket(player, joinPacket);
        }
    }

    private PacketPlayOutMount getEntityAttachPacket(int[] entityIDs) {
        return this.createDataSerializer(data -> {
            data.d(this.entityID);
            data.a(entityIDs);
            return new PacketPlayOutMount(data);
        });
    }

    private PacketPlayOutEntity.PacketPlayOutEntityLook getEntityLookPacket() {
        return new PacketPlayOutEntity.PacketPlayOutEntityLook(this.entityID, (byte) ((int) (this.location.getYaw() * 256.0F / 360.0F)), (byte) ((int) (this.location.getPitch() * 256.0F / 360.0F)), true);
    }

    private PacketPlayOutEntityTeleport getEntityTeleportPacket(boolean onGround) {
        return this.createDataSerializer(data -> {
            data.d(this.entityID);
            data.writeDouble(this.location.getX());
            data.writeDouble(this.location.getY());
            data.writeDouble(this.location.getZ());
            data.writeByte((byte) ((int) (this.location.getYaw() * 256.0F / 360.0F)));
            data.writeByte((byte) ((int) (this.location.getPitch() * 256.0F / 360.0F)));
            data.writeBoolean(onGround);
            return new PacketPlayOutEntityTeleport(data);
        });
    }

    private PacketPlayOutEntityHeadRotation getEntityHeadRotatePacket() {
        return this.createDataSerializer(data -> {
            data.d(this.entityID);
            data.writeByte((byte) ((int) (this.location.getYaw() * 256.0F / 360.0F)));
            return new PacketPlayOutEntityHeadRotation(data);
        });
    }

    private PacketPlayOutEntityEquipment getEntityEquipmentPacket(EnumItemSlot slot, ItemStack itemStack) {
        return new PacketPlayOutEntityEquipment(this.entityID, Collections.singletonList(new Pair<>(slot, itemStack)));
    }

    private PacketPlayOutAnimation getEntityAnimationPacket(Animation animation) {
        return this.createDataSerializer((data) -> {
            data.d(this.entityID);
            data.writeByte((byte) animation.getType());
            return new PacketPlayOutAnimation(data);
        });
    }

    private PacketPlayOutEntityDestroy getEntityDestroyPacket() {
        return new PacketPlayOutEntityDestroy(this.entityID);
    }

    private PacketPlayOutEntityMetadata getEntityMetadataPacket() {
        return this.createDataSerializer((data) -> {
            data.d(this.entityID);
            DataWatcher.a(this.metadata.getList(), data);
            return new PacketPlayOutEntityMetadata(data);
        });
    }

    private PacketPlayOutNamedEntitySpawn getEntitySpawnPacket() {
        return this.createDataSerializer((data) -> {
            data.d(this.entityID);
            data.a(this.profile.getId());
            data.writeDouble(this.location.getX());
            data.writeDouble(this.location.getY());
            data.writeDouble(this.location.getZ());
            data.writeByte((byte) ((int) (this.location.getYaw() * 256.0F / 360.0F)));
            data.writeByte((byte) ((int) (this.location.getPitch() * 256.0F / 360.0F)));
            return new PacketPlayOutNamedEntitySpawn(data);
        });
    }

    public PacketPlayOutPlayerInfo getPlayerInfoPacket(PlayerInfo playerInfo) {
        return this.createDataSerializer((data) -> {
            PacketPlayOutPlayerInfo.EnumPlayerInfoAction action = playerInfo.getPlayerInfo();
            PacketPlayOutPlayerInfo.PlayerInfoData playerInfoData = new PacketPlayOutPlayerInfo.PlayerInfoData(this.profile, this.ping.getMilliseconds(), this.gamemode.getGamemode(), CraftChatMessage.fromString(this.displayName)[0]);
            List<PacketPlayOutPlayerInfo.PlayerInfoData> list = Collections.singletonList(playerInfoData);
            data.a(playerInfo.getPlayerInfo());
            Method method = playerInfo.getPlayerInfo().getDeclaringClass().getDeclaredMethod("a", PacketDataSerializer.class, PacketPlayOutPlayerInfo.PlayerInfoData.class);
            method.setAccessible(true);
            data.a(list, (a, b) -> this.unsafe(() -> method.invoke(action, a, b)));
            return new PacketPlayOutPlayerInfo(data);
        });
    }

    public int getEntityID() {
        return entityID;
    }

    public GameProfile getProfile() {
        return profile;
    }

    public NPCMetaData getMetadata() {
        return metadata;
    }

    public Location getLocation() {
        return location;
    }

    public Ping getPing() {
        return ping;
    }

    public Gamemode getGameMode() {
        return gamemode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setSkin(SkinTextures skinTextures) {
        this.profile.getProperties().put("textures", new Property("textures", skinTextures.getTexture(), skinTextures.getSignature()));
    }

    public void setASyncSkinByUsername(Plugin plugin, Collection<Player> players, String username) {
        this.setASyncSkinByUsername(plugin, players, username, null);
    }

    public void setASyncSkinByUsername(Plugin plugin, Player player, String username) {
        this.setASyncSkinByUsername(plugin, player, username, null);
    }

    public void setASyncSkinByUUID(Plugin plugin, Collection<Player> players, UUID uuid) {
        this.setASyncSkinByUUID(plugin, players, uuid, null);
    }

    public void setASyncSkinByUUID(Plugin plugin, Player player, UUID uuid) {
        this.setASyncSkinByUUID(plugin, player, uuid, null);
    }

    public void setASyncSkinByUsername(Plugin plugin, Player player, String username, BiConsumer<Boolean, PlayerGhost> callback) {
        SkinTextures.getByUsername(plugin, username, (success, skin) -> setASyncSkin(success, skin, player, callback));
    }

    public void setASyncSkinByUsername(Plugin plugin, Collection<Player> players, String username, BiConsumer<Boolean, PlayerGhost> callback) {
        SkinTextures.getByUsername(plugin, username, (success, skin) -> setASyncSkin(success, skin, players, callback));
    }

    public void setASyncSkinByUUID(Plugin plugin, Player player, UUID uuid, BiConsumer<Boolean, PlayerGhost> callback) {
        SkinTextures.getByUUID(plugin, uuid, (success, skin) -> setASyncSkin(success, skin, player, callback));
    }

    public void setASyncSkinByUUID(Plugin plugin, Collection<Player> players, UUID uuid, BiConsumer<Boolean, PlayerGhost> callback) {
        SkinTextures.getByUUID(plugin, uuid, (success, skin) -> setASyncSkin(success, skin, players, callback));
    }

    private void setASyncSkin(boolean success, SkinTextures skin, Collection<Player> players, BiConsumer<Boolean, PlayerGhost> callback) {
        if (success) {
            this.setSkin(skin);
            this.reloadNPC(players);
        }
        callback.accept(success, this);
    }

    private void setASyncSkin(boolean success, SkinTextures skin, Player player, BiConsumer<Boolean, PlayerGhost> callback) {
        this.setASyncSkin(success, skin, Collections.singletonList(player), callback);
    }

    public void setPing(Ping ping) {
        this.ping = ping;
    }

    public void setGameMode(Gamemode gamemode) {
        this.gamemode = gamemode;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        GameProfile swapProfile = new GameProfile(this.profile.getId(), displayName);
        swapProfile.getProperties().putAll(this.profile.getProperties());
        this.profile = swapProfile;
    }

    public class NPCMetaData {

        //Entity metadata
        private final DataWatcher.Item<Byte> entityState = a(0, (byte) EntityState.createMask(EntityState.DEFAULT));
        private final DataWatcher.Item<Integer> airTicks = a(1, 300);
        private final DataWatcher.Item<Optional<IChatBaseComponent>> customName = a(2, Optional.empty(), DataWatcherRegistry.f);
        private final DataWatcher.Item<Boolean> customNameVisible = a(3, false);
        private final DataWatcher.Item<Boolean> silent = a(4, false);
        private final DataWatcher.Item<Boolean> gravity = a(5, false);
        private final DataWatcher.Item<EntityPose> pose = a(6, Pose.STANDING.getPose());
        private final DataWatcher.Item<Integer> frozenTicks = a(7, 0); //shaking at tick 140

        //LivingEntity metadata
        private final DataWatcher.Item<Byte> handStatus = a(8, (byte) HandStatus.createMask(HandStatus.MAIN_HAND));
        private final DataWatcher.Item<Float> health = a(9, 1.0F);
        private final DataWatcher.Item<Integer> potionEffectColor = a(10, 0);
        private final DataWatcher.Item<Boolean> isPotionEffectAmbient = a(11, false);
        private final DataWatcher.Item<Integer> arrowsInEntity = a(12, 0);
        private final DataWatcher.Item<Integer> absorptionHealth = a(13, 0);
        private final DataWatcher.Item<Optional<BlockPosition>> sleepingBedLocation = a(14, Optional.empty(), DataWatcherRegistry.m);

        //Player metadata
        private final DataWatcher.Item<Float> additionalHearts = a(15, 0.0F);
        private final DataWatcher.Item<Integer> score = a(16, 0);
        private final DataWatcher.Item<Byte> skinStatus = a(17, (byte) SkinStatus.createMask(SkinStatus.ALL_ENABLED));
        private final DataWatcher.Item<Byte> hand = a(18, (byte) Hand.RIGHT.getType());
        private final DataWatcher.Item<NBTTagCompound> leftShoulder = a(19, new NBTTagCompound());
        private final DataWatcher.Item<NBTTagCompound> rightShoulder = a(20, new NBTTagCompound());

        private final List<DataWatcher.Item<?>> list;

        public NPCMetaData() {
            this.list = new ArrayList<>(Arrays.asList(
                    this.entityState,
                    this.airTicks,
                    this.customName,
                    this.customNameVisible,
                    this.silent,
                    this.gravity,
                    this.pose,
                    this.frozenTicks,
                    this.handStatus,
                    this.health,
                    this.potionEffectColor,
                    this.isPotionEffectAmbient,
                    this.arrowsInEntity,
                    this.absorptionHealth,
                    this.sleepingBedLocation,
                    this.additionalHearts,
                    this.score,
                    this.skinStatus,
                    this.hand,
                    this.leftShoulder,
                    this.rightShoulder));
        }

        public EntityState[] getEntityState() {
            return EntityState.fromMask(entityState.b());
        }

        public Integer getAirTicks() {
            return airTicks.b();
        }

        public Optional<IChatBaseComponent> getCustomName() {
            return customName.b();
        }

        public Boolean isCustomNameVisible() {
            return customNameVisible.b();
        }

        public Boolean isSilent() {
            return silent.b();
        }

        public Boolean hasGravity() {
            return gravity.b();
        }

        public Pose getPose() {
            return Pose.fromPose(pose.b());
        }

        public Integer getFrozenTicks() {
            return frozenTicks.b();
        }

        public HandStatus[] getHandStatus() {
            return HandStatus.fromMask(handStatus.b());
        }

        public Float getHealth() {
            return health.b();
        }

        public Integer getPotionEffectColor() {
            return potionEffectColor.b();
        }

        public Boolean isPotionEffectAmbient() {
            return isPotionEffectAmbient.b();
        }

        public Integer getArrowsInEntity() {
            return arrowsInEntity.b();
        }

        public Integer getAbsorptionHealth() {
            return absorptionHealth.b();
        }

        public Optional<BlockPosition> getSleepingBedLocation() {
            return sleepingBedLocation.b();
        }

        public Float getAdditionalHearts() {
            return additionalHearts.b();
        }

        public Integer getScore() {
            return score.b();
        }

        public SkinStatus[] getSkinStatus() {
            return SkinStatus.fromMask(skinStatus.b());
        }

        public Hand getHand() {
            return Hand.fromByte(hand.b());
        }

        public NBTTagCompound getLeftShoulder() {
            return leftShoulder.b();
        }

        public NBTTagCompound getRightShoulder() {
            return rightShoulder.b();
        }

        public List<DataWatcher.Item<?>> getList() {
            return list;
        }

        public void setEntityState(EntityState... entityState) {
            this.entityState.a((byte) EntityState.createMask(entityState));
        }

        public void setAirTicks(Integer airTicks) {
            this.airTicks.a(airTicks);
        }

        public void setCustomName(String customName) {
            this.customName.a(Optional.ofNullable(IChatBaseComponent.a(customName)));
        }

        public void setCustomNameVisible(Boolean customNameVisible) {
            this.customNameVisible.a(customNameVisible);
        }

        public void setSilent(Boolean silent) {
            this.silent.a(silent);
        }

        public void setGravity(Boolean gravity) {
            this.gravity.a(gravity);
        }

        public void setPose(Pose pose) {
            this.pose.a(pose.getPose());
        }

        public void setFrozenTicks(Integer frozenTicks) {
            this.frozenTicks.a(frozenTicks);
        }

        public void setShaking() {
            this.setFrozenTicks(140);
        }

        public void setHandStatus(HandStatus handStatus) {
            this.handStatus.a((byte) HandStatus.createMask(handStatus));
        }

        public void setHealth(Float health) {
            this.health.a(health);
        }

        public void setPotionEffectColor(Integer potionEffectColor) {
            this.potionEffectColor.a(potionEffectColor);
        }

        public void setIsPotionEffectAmbient(Boolean isPotionEffectAmbient) {
            this.isPotionEffectAmbient.a(isPotionEffectAmbient);
        }

        public void setArrowsInEntity(Integer arrowsInEntity) {
            this.arrowsInEntity.a(arrowsInEntity);
        }

        public void setAbsorptionHealth(Integer absorptionHealth) {
            this.absorptionHealth.a(absorptionHealth);
        }

        public void setSleepingBedLocation(BlockPosition sleepingBedLocation) {
            this.sleepingBedLocation.a(Optional.ofNullable(sleepingBedLocation));
        }

        public void setAdditionalHearts(Float additionalHearts) {
            this.additionalHearts.a(additionalHearts);
        }

        public void setScore(Integer score) {
            this.score.a(score);
        }

        public void setSkinStatus(SkinStatus... skinStatus) {
            this.skinStatus.a((byte) SkinStatus.createMask(skinStatus));
        }

        public void setHand(Hand hand) {
            this.hand.a((byte) hand.getType());
        }

        public NBTTagCompound createParrot(Consumer<Parrot> callback, World world) {
            EntityParrot entityParrot = new EntityParrot(EntityTypes.al, ((CraftWorld) world).getHandle());
            CraftParrot parrot = new CraftParrot((CraftServer) Bukkit.getServer(), entityParrot);
            callback.accept(parrot);
            NBTTagCompound nbtTagCompound = new NBTTagCompound();
            entityParrot.d(nbtTagCompound);
            return nbtTagCompound;
        }

        public void setParrotLeftShoulder(Consumer<Parrot> callback, World world) {
            this.setLeftShoulder(this.createParrot(callback, world));
        }

        public void setParrotRightShoulder(Consumer<Parrot> callback, World world) {
            this.setRightShoulder(this.createParrot(callback, world));
        }

        public void setLeftShoulder(NBTTagCompound leftShoulder) {
            this.leftShoulder.a(leftShoulder);
        }

        public void setRightShoulder(NBTTagCompound rightShoulder) {
            this.rightShoulder.a(rightShoulder);
        }

        private static <T> DataWatcher.Item<T> a(int index, T value) {
            DataWatcherSerializer<?> serializer = null;

            if (value instanceof Byte) {
                serializer = DataWatcherRegistry.a;
            } else if (value instanceof Float) {
                serializer = DataWatcherRegistry.c;
            } else if (value instanceof Integer) {
                serializer = DataWatcherRegistry.b;
            } else if (value instanceof String) {
                serializer = DataWatcherRegistry.d;
            } else if (value instanceof Boolean) {
                serializer = DataWatcherRegistry.i;
            } else if (value instanceof NBTTagCompound) {
                serializer = DataWatcherRegistry.p;
            } else if (value instanceof BlockPosition) {
                serializer = DataWatcherRegistry.m;
            } else if (value instanceof IChatBaseComponent) {
                serializer = DataWatcherRegistry.e;
            } else if (value instanceof EntityPose) {
                serializer = DataWatcherRegistry.s;
            }
            return a(index, value, (DataWatcherSerializer<T>) serializer);
        }

        private static <T> DataWatcher.Item<T> a(int index, T value, DataWatcherSerializer<T> serializer) {
            return new DataWatcher.Item<>(new DataWatcherObject<>(index, serializer), value);
        }

    }

    public enum ItemSlot {

        MAIN_HAND(EnumItemSlot.a),
        OFF_HAND(EnumItemSlot.b),
        BOOTS(EnumItemSlot.c),
        LEGGINGS(EnumItemSlot.d),
        CHESTPLATE(EnumItemSlot.e),
        HELMET(EnumItemSlot.f);

        private final EnumItemSlot slot;

        ItemSlot(EnumItemSlot slot) {
            this.slot = slot;
        }

        public EnumItemSlot getSlot() {
            return slot;
        }
    }

    public enum Hand {

        LEFT(0),
        RIGHT(1);

        private final int type;

        Hand(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }

        public static Hand fromByte(byte type) {
            for (Hand hand : values()) {
                if (type == hand.type) {
                    return hand;
                }
            }
            return null;
        }
    }

    public enum Pose {

        STANDING(EntityPose.a),
        FALL_FLYING(EntityPose.b),
        SLEEPING(EntityPose.c),
        SWIMMING(EntityPose.d),
        SPIN_ATTACK(EntityPose.e),
        CROUCHING(EntityPose.f),
        LONG_JUMPING(EntityPose.g),
        DYING(EntityPose.h);

        private final EntityPose pose;

        Pose(EntityPose pose) {
            this.pose = pose;
        }

        public EntityPose getPose() {
            return pose;
        }

        public static Pose fromPose(EntityPose entityPose) {
            for (Pose pose : values()) {
                if (entityPose == pose.pose) {
                    return pose;
                }
            }
            return null;
        }
    }

    public enum SkinStatus {

        CAPE_ENABLED(0x01),
        JACKET_ENABLED(0x02),
        LEFT_SLEEVE_ENABLED(0x04),
        RIGHT_SLEEVE_ENABLED(0x08),
        LEFT_PANTS_LEG_ENABLED(0x10),
        RIGHT_PANTS_LEG_ENABLED(0x20),
        HAT_ENABLED(0x40),
        @Deprecated UNUSED(0x80),
        ALL_ENABLED(0xFF);

        private final int mask;

        SkinStatus(int mask) {
            this.mask = mask;
        }

        public int getMask() {
            return mask;
        }

        public static int createMask(SkinStatus... skinStatuses) {
            int mask = 0;
            for (SkinStatus handStatus : skinStatuses) {
                mask |= handStatus.mask;
            }
            return mask;
        }

        public static SkinStatus[] fromMask(int mask) {
            List<SkinStatus> list = new ArrayList<>();
            for (SkinStatus skinStatus : values()) {
                if ((skinStatus.mask & mask) == skinStatus.mask) {
                    list.add(skinStatus);
                }
            }
            return list.toArray(new SkinStatus[0]);
        }
    }

    public enum HandStatus {

        MAIN_HAND(0x00),
        HAND_ACTIVE(0x01),
        OFF_HAND(0x02),
        RIPTIDE_SPIN_ATTACK(0x04),
        ALL(0x07);

        private final int mask;

        HandStatus(int mask) {
            this.mask = mask;
        }

        public int getMask() {
            return mask;
        }

        public static int createMask(HandStatus... handStatuses) {
            int mask = 0;
            for (HandStatus handStatus : handStatuses) {
                mask |= handStatus.mask;
            }
            return mask;
        }

        public static HandStatus[] fromMask(int mask) {
            List<HandStatus> list = new ArrayList<>();
            for (HandStatus handStatus : values()) {
                if ((handStatus.mask & mask) == handStatus.mask) {
                    list.add(handStatus);
                }
            }
            return list.toArray(new HandStatus[0]);
        }
    }

    public enum Animation {

        SWING_MAIN_HAND(0),
        TAKE_DAMAGE(1),
        LEAVE_BED(2),
        SWING_OFFHAND(3),
        CRITICAL_EFFECT(4),
        MAGIC_CRITICAL_EFFECT(5);

        private final int type;

        Animation(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }

    }

    public enum EntityState {

        DEFAULT(0x00),
        ON_FIRE(0x01),
        @Deprecated CROUCHING(0x02),
        @Deprecated UNUSED(0x04),
        SPRINTING(0x08),
        SWIMMING(0x10),
        INVISIBLE(0x20),
        GLOWING(0x40),
        FLYING(0x80),
        ALL(0xFF);

        private final int mask;

        EntityState(int mask) {
            this.mask = mask;
        }

        public int getMask() {
            return mask;
        }

        public static int createMask(EntityState... entityStates) {
            int mask = 0;
            for (EntityState entityState : entityStates) {
                mask |= entityState.mask;
            }
            return mask;
        }

        public static EntityState[] fromMask(int mask) {
            List<EntityState> list = new ArrayList<>();
            for (EntityState entityState : values()) {
                if ((entityState.mask & mask) == entityState.mask) {
                    list.add(entityState);
                }
            }
            return list.toArray(new EntityState[0]);
        }
    }

    public enum Gamemode {

        SURVIVAL(EnumGamemode.a),
        CREATIVE(EnumGamemode.b),
        ADVENTURE(EnumGamemode.c),
        SPECTATOR(EnumGamemode.d);

        private final EnumGamemode gamemode;

        Gamemode(EnumGamemode gamemode) {
            this.gamemode = gamemode;
        }

        public EnumGamemode getGamemode() {
            return gamemode;
        }
    }

    public enum PlayerInfo {

        ADD_PLAYER(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.a),
        UPDATE_GAME_MODE(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.b),
        UPDATE_LATENCY(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.c),
        UPDATE_DISPLAY_NAME(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.d),
        REMOVE_PLAYER(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.e);

        private final PacketPlayOutPlayerInfo.EnumPlayerInfoAction playerInfo;

        PlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction a) {
            this.playerInfo = a;
        }

        public PacketPlayOutPlayerInfo.EnumPlayerInfoAction getPlayerInfo() {
            return playerInfo;
        }
    }

    public enum Ping {

        NO_CONNECTION(-1),
        ONE_BAR(1000),
        TWO_BARS(999),
        THREE_BARS(599),
        FOUR_BARS(299),
        FIVE_BARS(149);

        private final int milliseconds;

        Ping(int milliseconds) {
            this.milliseconds = milliseconds;
        }

        public int getMilliseconds() {
            return milliseconds;
        }
    }

    public static class SkinTextures {

        private static final String TEXTURE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";
        private static final String UUID_URL = "https://api.mojang.com/profiles/minecraft";

        private final String texture;
        private final String signature;

        public SkinTextures(String textures, String signature) {
            this.texture = textures;
            this.signature = signature;
        }

        public String getTexture() {
            return texture;
        }

        public String getSignature() {
            return signature;
        }

        public static void getByUsername(Plugin plugin, String username, BiConsumer<Boolean, SkinTextures> callback) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    JSONArray array = new JSONArray();
                    array.add(username);
                    UUID result = null;

                    try {
                        HttpRequest request = HttpRequest.newBuilder(new URI(UUID_URL))
                                .setHeader("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(array.toString()))
                                .timeout(Duration.ofSeconds(5))
                                .build();

                        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                            JSONArray uuidArray = (JSONArray) new JSONParser().parse(response.body());
                            if (!uuidArray.isEmpty()) {
                                String uuidStr = (String) ((JSONObject) uuidArray.get(0)).get("id");
                                result = UUID.fromString(uuidStr.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                            }
                        }

                    } catch (URISyntaxException | InterruptedException | IOException | ParseException e) {
                        e.printStackTrace();
                    }

                    if (result == null) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                callback.accept(false, null);
                            }
                        }.runTask(plugin);
                    } else {
                        SkinTextures.getByUUID(plugin, result, callback);
                    }
                }
            }.runTaskAsynchronously(plugin);
        }

        public static void getByUUID(Plugin plugin, UUID uuid, BiConsumer<Boolean, SkinTextures> callback) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    SkinTextures result = null;

                    try {
                        HttpRequest request = HttpRequest.newBuilder(new URI(String.format(TEXTURE_URL, uuid.toString().replace("-", ""))))
                                .timeout(Duration.ofSeconds(5))
                                .GET()
                                .build();

                        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

                        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                            JSONArray properties = (JSONArray) ((JSONObject) new JSONParser().parse(response.body())).get("properties");
                            for (Object property : properties) {
                                JSONObject obj = (JSONObject) property;
                                if (obj.containsKey("name") && obj.get("name").equals("textures")) {
                                    result = new SkinTextures((String) obj.get("value"), (String) obj.get("signature"));
                                }
                            }
                        }

                    } catch (URISyntaxException | InterruptedException | IOException | ParseException e) {
                        e.printStackTrace();
                    }

                    final SkinTextures skinTextures = result;

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (skinTextures == null) {
                                callback.accept(false, null);
                            } else {
                                callback.accept(true, skinTextures);
                            }
                        }
                    }.runTask(plugin);
                }
            }.runTaskAsynchronously(plugin);
        }
    }

    private void unsafe(UnsafeRunnable run) {
        try {
            run.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private <T> T createDataSerializer(UnsafeFunction<PacketDataSerializer, T> callback) {
        PacketDataSerializer data = new PacketDataSerializer(Unpooled.buffer());
        T result = null;
        try {
            result = callback.apply(data);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.release();
        }
        return result;
    }

    @FunctionalInterface
    private interface UnsafeRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface UnsafeFunction<K, T> {
        T apply(K k) throws Exception;
    }

}
