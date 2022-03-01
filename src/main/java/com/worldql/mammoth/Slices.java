package com.worldql.mammoth;

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

        int currentOwner = MammothPlugin.mammothServerId;

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

    public static int getDistanceFromSliceBoundary(Location l) {
        int smallest = 99999;

        boolean distanceSetRelativeToUnslicedOriginArea = false;

        if (MammothPlugin.avoidSlicingOrigin) {
            int r = MammothPlugin.originRadius;
            int r_max = r + dmzSize;
            int r_min = r - dmzSize;
            double x = Math.abs(l.getX());
            double z = Math.abs(l.getZ());

            if (x > r && x < r_max) {
                int distance = (int) (x-r);
                if (distance < smallest) {
                    smallest = distance;
                    distanceSetRelativeToUnslicedOriginArea = true;
                }
            }
            if (z > r && z < r_max) {
                int distance = (int) (z-r);
                if (distance < smallest) {
                    smallest = distance;
                    distanceSetRelativeToUnslicedOriginArea = true;
                }
            }
            if (x < r && x > r_min) {
                int distance = (int) (r-x);
                if (distance < smallest) {
                    smallest = distance;
                    distanceSetRelativeToUnslicedOriginArea = true;
                }
            }
            if (z < r && z > r_min) {
                int distance = (int) (r-z);
                if (distance < smallest) {
                    smallest = distance;
                    distanceSetRelativeToUnslicedOriginArea = true;
                }
            }
        }

        if (distanceSetRelativeToUnslicedOriginArea) {
            return smallest;
        }


        int adjustedX = (int) (l.getX() + (worldDiameter / 2));
        int adjustedZ = (int) (l.getZ() + (worldDiameter / 2));
        int xModSliceWidth = adjustedX % sliceWidth;
        int zModSliceWidth = adjustedZ % sliceWidth;
        int upper = sliceWidth - dmzSize;

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

    private static boolean isInUnslicedOrigin(Location l) {
        if (MammothPlugin.avoidSlicingOrigin) {
            int r = MammothPlugin.originRadius;
            double x = Math.abs(l.getX());
            double z = Math.abs(l.getZ());
            return x < r && z < r;
        }
        return false;
    }

    public static boolean isDMZ(Location l) {
        if (MammothPlugin.avoidSlicingOrigin) {
            int r = MammothPlugin.originRadius;
            int r_max = r + dmzSize;
            int r_min = r - dmzSize;
            double x = Math.abs(l.getX());
            double z = Math.abs(l.getZ());

            if (x < r_max && z < r_max) {
                if (x >= r && x < r_max) {
                    return true;
                }
                if (z >= r && z < r_max) {
                    return true;
                }
                if (x < r && x > r_min) {
                    return true;
                }
                if (z < r && z > r_min) {
                    return true;
                }
            }

            if (isInUnslicedOrigin(l)) {
                return false;
            }
        }

        if (l.getWorld().getName().equals(MammothPlugin.worldName + "_the_end")) {
            return false;
        }

        int adjustedX = (int) (l.getX() + (worldDiameter / 2));
        int adjustedZ = (int) (l.getZ() + (worldDiameter / 2));
        int xModSliceWidth = adjustedX % sliceWidth;
        int zModSliceWidth = adjustedZ % sliceWidth;
        int upper = sliceWidth - dmzSize;

        if (xModSliceWidth < dmzSize || xModSliceWidth > upper) {
            return true;
        }
        if (zModSliceWidth < dmzSize || zModSliceWidth > upper) {
            return true;
        }

        return false;
    }

    public static int getOwnerOfLocation(Location l) {
        if (isInUnslicedOrigin(l)) {
            return 0;
        }

        if (l.getWorld().getName().equalsIgnoreCase(MammothPlugin.worldName + "_the_end")) {
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
