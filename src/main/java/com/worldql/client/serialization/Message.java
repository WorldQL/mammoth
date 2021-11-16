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
        @Nullable Vec3D position,
        @Nullable List<Record> records,
        @Nullable List<Entity> entities,
        @Nullable String parameter,
        @Nullable ByteBuffer flex
) {
    public Message(Instruction instruction, @NotNull UUID senderUuid, @NotNull String worldName) {
        this(instruction, senderUuid, worldName, null, null, null, null, null);
    }

    public Message(Instruction instruction, @NotNull UUID senderUuid, @NotNull String worldName, @Nullable Vec3D position) {
        this(instruction, senderUuid, worldName, position, null, null, null, null);
    }

    public byte[] encode() {
        return Codec.encodeMessage(this);
    }

    public static Message decode(@NotNull ByteBuffer buf) {
        return Codec.decodeMessage(buf);
    }
}
