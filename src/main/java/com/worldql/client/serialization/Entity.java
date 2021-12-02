package com.worldql.client.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.UUID;

import static com.worldql.client.serialization.Codec.copyByteBuffer;

public record Entity(
        @NotNull UUID uuid,
        @NotNull Vec3D position,
        @NotNull String worldName,
        @Nullable String data,
        @Nullable ByteBuffer flex
) {
    public Entity(@NotNull UUID uuid, @NotNull Vec3D position, @NotNull String worldName) {
        this(uuid, position, worldName, null, null);
    }

    public Entity copy() {
        return new Entity(uuid(), position(), worldName(), data(), copyByteBuffer(flex()));
    }

    public Entity withUuid(@NotNull UUID uuid) {
        return new Entity(uuid, position(), worldName(), data(), copyByteBuffer(flex()));
    }

    public Entity withPosition(@NotNull Vec3D position) {
        return new Entity(uuid(), position, worldName(), data(), copyByteBuffer(flex()));
    }

    public Entity withWorldName(@NotNull String worldName) {
        return new Entity(uuid(), position(), worldName, data(), copyByteBuffer(flex()));
    }

    public Entity withData(@Nullable String data) {
        return new Entity(uuid(), position(), worldName(), data, copyByteBuffer(flex()));
    }

    public Entity withFlex(@Nullable ByteBuffer flex) {
        return new Entity(uuid(), position(), worldName(), data(), flex);
    }
}
