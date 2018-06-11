package com.example.hrdp.viewer;

import android.os.Environment;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by Mansu on 2016-07-23.
 */
public class Config {
    public static String serverIp = "192.168.0.27";
    public static String clientIp = "192.168.0.23";
    public static InetAddress serverAddress;

    //Socket
    public static ReliableSock reliableSock = null;
    public static NonReliableSock nonReliableSock = null;

    public static int serverReliablePort = 3001;

    public static int clientReliablePort = 3001;
    public static int clientReliableTimeout = 2000;
    public static int clientReliableSleepTime = 30;
    public static int clientReliableSegSize = 200;

    public static int clientNonReliablePort = 3002;
    public static int clientNonReliableTimeout = 1000;
    public static int clientNonReliableSleepTime = 30;
    public static int clientNonReliableSegSize = 200;

    //video
    public static int videoWidth = 640;
    public static int videoHeight = 480;
    public static int videoResizeWidth = 160;
    public static int videoResizeHeight = 120;
    public static int bitRate = 50*1024;
    public static int frameRate = 10;
    public static int keyFrameRate = 40;
    public static int crf = 34;
    public static String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/videoPath";
    public static String configPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/configPath";

    //frActivity
    public static String frPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/frpath/";
    public static int maxNode = 200;

    public static byte[] toBytes(int i) {
        byte[] result = new byte[4];

        result[0] = (byte) (i >> 24);
        result[1] = (byte) (i >> 16);
        result[2] = (byte) (i >> 8);
        result[3] = (byte) (i /*>> 0*/);

        return result;
    }

    public static int toInt(byte[] data, int offset) {
        return (data[offset] & 0xff) << 24 | (data[1+offset] & 0xff) << 16 |
                (data[2+offset] & 0xff) << 8  | (data[3+offset] & 0xff);
    }

    public static short toShort(byte[] data, int offset) {
        return (short)((data[offset] & 0xff) << 8 | (data[1+offset] & 0xff));
    }

    public static byte[] LongtoBytes(long i) {
        byte[] result = new byte[8];

        result[0] = (byte) (i >> 56);
        result[1] = (byte) (i >> 48);
        result[2] = (byte) (i >> 40);
        result[3] = (byte) (i >> 32);
        result[4] = (byte) (i >> 24);
        result[5] = (byte) (i >> 16);
        result[6] = (byte) (i >> 8);
        result[7] = (byte) (i /*>> 0*/);

        return result;
    }

    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }
}