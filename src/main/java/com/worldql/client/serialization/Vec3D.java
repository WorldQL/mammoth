package com.worldql.client.serialization;

import com.worldql.client.Messages.Vec3d;
import org.bukkit.Location;

public record Vec3D(double x, double y, double z) {
    public Vec3D(Vec3d raw) {
        this(raw.x(), raw.y(), raw.z());
    }

    public Vec3D(Location location) {
        this(location.getX(), location.getY(), location.getZ());
    }
}
