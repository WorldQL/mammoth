package org.nqnl.mammothgameserver;
import java.lang.Math;

public class SliceMethods {
    private static int SLICE_SIZE = 1024;
    private static int NUMBER_OF_SERVERS = 7;
    private static int DMZ_SIZE = 10;

    private static int getSlice(double x) {
        return (int) Math.floor(x / SLICE_SIZE);
    }
    private static int getServerId(int slice){
        return Math.abs(slice) % NUMBER_OF_SERVERS;
    }

    public static int getServerIdFromX(double x) {
        return getServerId(getSlice(x));
    }

    public static boolean getDMZStatus(double x) {
        int xc = (int) Math.floor(Math.abs(x));
        int m = xc % SLICE_SIZE;
        if (m < DMZ_SIZE) {
            return true;
        }
        if (m > SLICE_SIZE - DMZ_SIZE) {
            return true;
        }
        return false;
    }

    // only called inside DMZ so uses silly safe magic numbers.
    public static int getNearestNeighbor(double x) {
        int direction = 50;
        if (x < 0) {
            direction = -50;
        }
        int xc = (int) Math.floor(Math.abs(x));
        int m = xc % SLICE_SIZE;
        if (m < DMZ_SIZE + 10) {
            return getServerIdFromX(x - direction);
        }
        if (m > SLICE_SIZE - DMZ_SIZE - 10) {
            return getServerIdFromX(x + direction);
        }
        return -1;
    }
}
