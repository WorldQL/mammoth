package com.worldql.client;

import org.bukkit.Location;

public class Slices {
    public static boolean enabled = true;
    public static int numServers = 2;
    public static int worldDiameter = 2000;
    public static int sliceWidth = 1000;
    public static int dmzSize = 4;

    public static CrossDirection getShoveDirection(Location playerLocation) {
        int northOwner = getOwnerOfLocation(playerLocation.clone().add(0, 0, -5));
        int southOwner = getOwnerOfLocation(playerLocation.clone().add(0, 0, 5));
        int westOwner = getOwnerOfLocation(playerLocation.clone().add(-5, 0, 0));
        int eastOwner = getOwnerOfLocation(playerLocation.clone().add(5, 0, 0));

        int currentOwner = WorldQLClient.mammothServerId;

        if (northOwner == currentOwner) {
            return CrossDirection.NORTH_NEGATIVE_Z;
        }
        if (southOwner == currentOwner) {
            return CrossDirection.SOUTH_POSITIVE_Z;
        }
        if (westOwner == currentOwner) {
            return CrossDirection.WEST_NEGATIVE_X;
        }
        if (eastOwner == currentOwner) {
            return CrossDirection.EAST_POSITIVE_X;
        }

        return CrossDirection.ERROR;
    }

    public static int getDistanceFromDMZ(Location l) {
        int adjustedX = (int) (l.getX() + (worldDiameter / 2));
        int adjustedZ = (int) (l.getZ() + (worldDiameter / 2));
        int xModSliceWidth = adjustedX % sliceWidth;
        int zModSliceWidth = adjustedZ % sliceWidth;
        int upper = sliceWidth - dmzSize;
        int smallest = 99999;

        if (xModSliceWidth < dmzSize) {
            if (xModSliceWidth < smallest) {
                smallest = xModSliceWidth;
            }
        } else if (xModSliceWidth > upper) {
            int distance = sliceWidth - xModSliceWidth;
            if (distance < smallest) {
                smallest = distance;
            }
        }

        if (zModSliceWidth < dmzSize) {
            if (zModSliceWidth < smallest) {
                smallest = zModSliceWidth;
            }
        } else if (zModSliceWidth > upper) {
            int distance = sliceWidth - zModSliceWidth;
            if (distance < smallest) {
                smallest = distance;
            }
        }

        return smallest;
    }

    public static boolean isDMZ(Location l) {
        int adjustedX = (int) (l.getX() + (worldDiameter / 2));
        int adjustedZ = (int) (l.getZ() + (worldDiameter / 2));

        int distanceFromXEdge = adjustedX % sliceWidth;
        int distanceFromZEdge = adjustedZ % sliceWidth;

        int upper = sliceWidth - dmzSize;

        if (distanceFromXEdge < dmzSize || distanceFromXEdge > upper) {
            return true;
        }
        if (distanceFromZEdge < dmzSize || distanceFromZEdge > upper) {
            return true;
        }

        return false;
    }

    public static int getOwnerOfLocation(Location l) {
        if (l.getWorld().getName().equalsIgnoreCase("world_the_end")) {
            return 0;
        }

        int slicesPerRow = worldDiameter / sliceWidth;

        int adjustedX = (int) (l.getX() + (worldDiameter / 2));
        int adjustedZ = (int) (l.getZ() + (worldDiameter / 2));

        int sliceX = adjustedX / sliceWidth;
        int sliceZ = adjustedZ / sliceWidth;


        int position = sliceX + (sliceZ * slicesPerRow);
        return position % numServers;
    }
}
