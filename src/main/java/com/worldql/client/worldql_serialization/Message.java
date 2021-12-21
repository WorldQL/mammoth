package com.worldql.client.worldql_serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

import static com.worldql.client.worldql_serialization.Codec.copyByteBuffer;
import static com.worldql.client.worldql_serialization.Codec.copyList;

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
    public Message copy() {
        return new Message(
                instruction(),
                senderUuid(),
                worldName(),
                replication(),
                position(),
                copyList(records(), Record::copy),
                copyList(entities(), Entity::copy),
                parameter(),
                copyByteBuffer(flex())
        );
    }

    public Message withInstruction(Instruction instruction) {
        return new Message(
                instruction,
                senderUuid(),
                worldName(),
                replication(),
                position(),
                copyList(records(), Record::copy),
                copyList(entities(), Entity::copy),
                parameter(),
                copyByteBuffer(flex())
        );
    }

    public Message withSenderUuid(@NotNull UUID senderUuid) {
        return new Message(
                instruction(),
                senderUuid,
                worldName(),
                replication(),
                position(),
                copyList(records(), Record::copy),
                copyList(entities(), Entity::copy),
                parameter(),
                copyByteBuffer(flex())
        );
    }

    public Message withWorldName(@NotNull String worldName) {
        return new Message(
                instruction(),
                senderUuid(),
                worldName,
                replication(),
                position(),
                copyList(records(), Record::copy),
                copyList(entities(), Entity::copy),
                parameter(),
                copyByteBuffer(flex())
        );
    }

    public Message withReplication(Replication replication) {
        return new Message(
                instruction(),
                senderUuid(),
                worldName(),
                replication,
                position(),
                copyList(records(), Record::copy),
                copyList(entities(), Entity::copy),
                parameter(),
                copyByteBuffer(flex())
        );
    }

    public Message withPosition(@Nullable Vec3D position) {
        return new Message(
                instruction(),
                senderUuid(),
                worldName(),
                replication(),
                position,
                copyList(records(), Record::copy),
                copyList(entities(), Entity::copy),
                parameter(),
                copyByteBuffer(flex())
        );
    }

    public Message withRecords(@Nullable List<Record> records) {
        return new Message(
                instruction(),
                senderUuid(),
                worldName(),
                replication(),
                position(),
                records,
                copyList(entities(), Entity::copy),
                parameter(),
                copyByteBuffer(flex())
        );
    }

    public Message withEntities(@Nullable List<Entity> entities) {
        return new Message(
                instruction(),
                senderUuid(),
                worldName(),
                replication(),
                position(),
                copyList(records(), Record::copy),
                entities,
                parameter(),
                copyByteBuffer(flex())
        );
    }

    public Message withParameter(@Nullable String parameter) {
        return new Message(
                instruction(),
                senderUuid(),
                worldName(),
                replication(),
                position(),
                copyList(records(), Record::copy),
                copyList(entities(), Entity::copy),
                parameter,
                copyByteBuffer(flex())
        );
    }

    public Message withFlex(@Nullable ByteBuffer flex) {
        return new Message(
                instruction(),
                senderUuid(),
                worldName(),
                replication(),
                position(),
                copyList(records(), Record::copy),
                copyList(entities(), Entity::copy),
                parameter(),
                flex
        );
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
