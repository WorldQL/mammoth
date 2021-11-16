package com.worldql.client.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.UUID;

public record Record(
        @NotNull UUID uuid,
        @NotNull Vec3D position,
        @NotNull String worldName,
        @Nullable String data,
        @Nullable ByteBuffer flex
) {
    public Record(@NotNull UUID uuid, @NotNull Vec3D position, @NotNull String worldName) {
        this(uuid, position, worldName, null, null);
    }
}
