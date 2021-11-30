package com.worldql.client.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

public record Message(
        Instruction instruction,
        @NotNull UUID senderUuid,
        @NotNull String worldName,
        Replication replication,
        @Nullable Vec3D position,
        @Nullable List<Record> records,
        @Nullable List<Entity> entities,
        @Nullable String parameter,
        @Nullable ByteBuffer flex
) {
    // region: Constructors
    public Message(Instruction instruction, @NotNull UUID senderUuid, @NotNull String worldName) {
        this(instruction, senderUuid, worldName, Replication.ExceptSelf, null, null, null, null, null);
    }

    public Message(Instruction instruction, @NotNull UUID senderUuid, @NotNull String worldName, @Nullable Vec3D position) {
        this(instruction, senderUuid, worldName, Replication.ExceptSelf, position, null, null, null, null);
    }

    public Message(Instruction instruction, @NotNull UUID senderUuid, @NotNull String worldName, Replication replication) {
        this(instruction, senderUuid, worldName, replication, null, null, null, null, null);
    }

    public Message(Instruction instruction, @NotNull UUID senderUuid, @NotNull String worldName, Replication replication, @Nullable Vec3D position) {
        this(instruction, senderUuid, worldName, replication, position, null, null, null, null);
    }
    // endregion

    // region: Wither Functions
    public Message withInstruction(Instruction instruction) {
        return new Message(instruction, senderUuid(), worldName(), replication(), position(), records(), entities(), parameter(), flex());
    }

    public Message withSenderUuid(@NotNull UUID senderUuid) {
        return new Message(instruction(), senderUuid, worldName(), replication(), position(), records(), entities(), parameter(), flex());
    }

    public Message withWorldName(@NotNull String worldName) {
        return new Message(instruction(), senderUuid(), worldName, replication(), position(), records(), entities(), parameter(), flex());
    }

    public Message withReplication(Replication replication) {
        return new Message(instruction(), senderUuid(), worldName(), replication, position(), records(), entities(), parameter(), flex());
    }

    public Message withPosition(@Nullable Vec3D position) {
        return new Message(instruction(), senderUuid(), worldName(), replication(), position, records(), entities(), parameter(), flex());
    }

    public Message withRecords(@Nullable List<Record> records) {
        return new Message(instruction(), senderUuid(), worldName(), replication(), position(), records, entities(), parameter(), flex());
    }

    public Message withEntities(@Nullable List<Entity> entities) {
        return new Message(instruction(), senderUuid(), worldName(), replication(), position(), records(), entities, parameter(), flex());
    }

    public Message withParameter(@Nullable String parameter) {
        return new Message(instruction(), senderUuid(), worldName(), replication(), position(), records(), entities(), parameter, flex());
    }

    public Message withFlex(@Nullable ByteBuffer flex) {
        return new Message(instruction(), senderUuid(), worldName(), replication(), position(), records(), entities(), parameter(), flex);
    }
    // endregion

    // region: Codec
    public byte[] encode() {
        return Codec.encodeMessage(this);
    }

    public static Message decode(@NotNull ByteBuffer buf) {
        return Codec.decodeMessage(buf);
    }
    // endregion
}
