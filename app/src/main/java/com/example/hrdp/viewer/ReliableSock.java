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
public class ReliableSock {
    private String TAG = "ReliableSock";
    private DatagramSocket mSock = null;
    private boolean isTimeout = false;
    private int segSize = 0;
    private int sleepTime = 0;
    private int timeout;

    public ReliableSock(int port, InetAddress ip, int m_segSize, int m_sleepTime, int timeout) {
        try {
            mSock = new DatagramSocket(null);
            mSock.setReuseAddress(true);
            mSock.bind(new InetSocketAddress(ip, port));
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
            try {
                byte[] seg = new byte[segSize];
                while (true) {
                    DatagramPacket pkt = new DatagramPacket(seg, seg.length);
                    mSock.receive(pkt);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean send(byte[] buf, InetAddress ip, int port) {
        int i = 0;
        while(true) {
            try {
                for (; i < buf.length / segSize; i++) {
                    DatagramPacket pkt = new DatagramPacket(buf, i * segSize, segSize, ip, port);
                    mSock.send(pkt);
                    Log.d(TAG, "send!!");
                    Thread.sleep(sleepTime);
                }
                DatagramPacket pkt = new DatagramPacket(buf, i * segSize, buf.length - i * segSize, ip, port);
                mSock.send(pkt);
                Thread.sleep(sleepTime);
                i++;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                byte[] response = new byte[5];
                DatagramPacket pkt = new DatagramPacket(response, response.length);

                try {
                    mSock.receive(pkt);
                    if(!pkt.getAddress().equals(ip))
                        return false;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }

                Log.d(TAG, "[0]: "+response[0]+"[1]: "+response[1]+"[2]: "+response[2]+"[3]: "+response[3]+"[4]: "+response[4]);
                if(response[0] == 0x34 && response[1] == 0x56 && response[2] == 0x78) {
                    if(response[3] == 0x00)
                        break;
                    else if(response[3] == 0x01)
                        i = response[4];
                }
            }
        }
        return true;
    }

    public byte[] recv(int size, InetAddress ip, int port) {
        byte[] recvBuf = new byte[size];

        int i = 0;
        int count = 0;
        while(true) {
            try {
                for (; i < size / segSize; i++) {
                    DatagramPacket pkt = new DatagramPacket(recvBuf, i * segSize, segSize);
                    mSock.receive(pkt);
                    if(!pkt.getAddress().equals(ip))
                        return new byte[0];
                    Log.d(TAG, "recved!! "+i);
                }
                DatagramPacket pkt = new DatagramPacket(recvBuf, i * segSize, size - i * segSize);
                mSock.receive(pkt);
                if(!pkt.getAddress().equals(ip))
                    return new byte[0];
                i++;
            } catch (IOException e) {
                e.printStackTrace();
                count++;
            }

            if(i > 0) {
                if (i == 1 + size / segSize) {
                    byte[] response = new byte[]{0x34, 0x56, 0x78, 0x00, 0x00};
                    DatagramPacket pkt = new DatagramPacket(response, response.length, ip, port);

                    try {
                        mSock.send(pkt);
                        System.out.println("send1!!");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                } else {
                    byte[] response = new byte[]{0x34, 0x56, 0x78, 0x01, 0x00};
                    DatagramPacket pkt = new DatagramPacket(response, response.length, ip, port);
                    response[4] = (byte) i;

                    try {
                        mSock.send(pkt);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (count >= 10)
                    return new byte[0];
            }
            else
                return new byte[0];
        }
        return recvBuf;
    }
}