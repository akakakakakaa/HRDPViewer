package com.example.hrdp.viewer;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * Created by yj on 16. 9. 13.
 */
public class NonReliableSock {
    private String TAG = "ReliableSock";
    private DatagramSocket mSock = null;
    private boolean isTimeout = false;
    private int segSize = 0;
    private int sleepTime = 0;
    private int port;
    private InetAddress ip;
    private int timeout;

    public NonReliableSock(int port, InetAddress ip, int m_segSize, int m_sleepTime, int timeout) {
        try {
            mSock = new DatagramSocket(null);
            mSock.setReuseAddress(true);
            mSock.bind(new InetSocketAddress(ip, port));
            this.port = port;
            this.ip = ip;
            segSize = m_segSize;
            sleepTime = m_sleepTime;
            this.timeout = timeout;
        } catch(SocketException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        mSock.close();
    }

    public void setSoTimeout(int timeout) {
        try {
            mSock.setSoTimeout(timeout);
            isTimeout = true;
        } catch(SocketException e) {
            e.printStackTrace();
        }
    }

    //if
    public void clear() {
        if(isTimeout == true) {
            setSoTimeout(1);
            try {
                byte[] seg = new byte[segSize];
                while (true) {
                    long start = System.currentTimeMillis();
                    DatagramPacket pkt = new DatagramPacket(seg, seg.length);
                    mSock.receive(pkt);
                    System.out.println(System.currentTimeMillis() - start);
                }
            } catch(IOException e) {
                e.printStackTrace();
            }
            setSoTimeout(timeout);
        }
    }

    public boolean send(byte[] buf, InetAddress ip, int port) {
        int i;
        try {
            for(i=0; i<buf.length /segSize; i++) {
                DatagramPacket pkt = new DatagramPacket(buf, i * segSize, segSize, ip, port);
                mSock.send(pkt);
                Thread.sleep(sleepTime);
            }
            if(buf.length - i*segSize != 0) {
                DatagramPacket pkt = new DatagramPacket(buf, i * segSize, buf.length - i * segSize, ip, port);
                mSock.send(pkt);
                Thread.sleep((int) (sleepTime * (buf.length - i * segSize) / (float) segSize));
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public byte[] recv(int size, InetAddress ip) {
        byte[] recvBuf = new byte[size];

        int i;
        int realSize = 0;
        try {
            for(i=0; i<size/segSize; i++) {
                DatagramPacket pkt = new DatagramPacket(recvBuf, i*segSize, segSize);
                mSock.receive(pkt);
                realSize += pkt.getLength();
                if(!pkt.getAddress().equals(ip))
                    return new byte[0];
            }

            if(size - i*segSize != 0) {
                DatagramPacket pkt = new DatagramPacket(recvBuf, i * segSize, size - i * segSize);
                mSock.receive(pkt);
                realSize += pkt.getLength();
                if (!pkt.getAddress().equals(ip))
                    return new byte[0];
            }

            if(size != realSize) {
                byte[] realBuf = new byte[realSize];
                System.arraycopy(recvBuf, 0, realBuf, 0, realSize);
                Log.d(TAG, "realSize = " + realSize + " size = " + size);
                return realBuf;
            }
            else
                return recvBuf;
        } catch(IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}