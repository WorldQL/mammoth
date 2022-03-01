package com.worldql.mammoth.worldql_serialization;

import com.worldql.mammoth.Messages.Vec3d;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public record Vec3D(double x, double y, double z) {
    public Vec3D(@NotNull Vec3d raw) {
        this(raw.x(), raw.y(), raw.z());
    }

    public Vec3D(@NotNull Location location) {
        this(location.getX(), location.getY(), location.getZ());
    }

    public Vec3D withX(double x) {
        return new Vec3D(x, y(), z());
    }

    public Vec3D withY(double y) {
        return new Vec3D(x(), y, z());
    }

    public Vec3D withZ(double z) {
        return new Vec3D(x(), y(), z);
    }
}
