package com.example.hrdp.viewer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

/**
 * Created by yj on 16. 9. 8.
 */
public class ViewThread extends Thread {
    private String TAG = "ViewThread";
    private Activity viewActivity = null;
    private Surface viewSurface = null;

    //flag
    private boolean isSet = false;
    private boolean isStop = false;
    private boolean isPause = false;
    private boolean isFile = false;

    //lock
    private Object pauseLock = null;

    //check
    private int strangeCount = 0;

    //file
    private ArrayList<Long> timestamps = new ArrayList<>();

    //
    private List<Rect> currentRectList = null;
    private int rectClear = 0;
    private boolean isDrawRect = false;

    private static ViewThread viewThread = null;

    public static ViewThread getInstance() {
        if(viewThread == null)
            viewThread = new ViewThread();
        return viewThread;
    }

    public void setView(Activity activity) { viewActivity = activity; }

    public void setSurface(Surface surface) {
        if(viewSurface == null)
            viewSurface = surface;
        else {
            viewSurface = surface;
            Codec.surface_init(viewSurface, Config.videoWidth, Config.videoHeight);
        }
    }

    public ViewThread() {
        viewThread = this;
        pauseLock = new Object();
        try {
            Config.serverAddress = InetAddress.getByName(Config.serverIp);
            settingReliableUDP(Config.clientReliableTimeout);
            settingNonReliableUDP(Config.clientNonReliableTimeout);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void settingReliableUDP(int timeout) throws IOException {
        Config.reliableSock = new ReliableSock(Config.clientReliablePort, InetAddress.getByName(Config.clientIp), Config.clientReliableSegSize, Config.clientReliableSleepTime, timeout);
        Config.reliableSock.setSoTimeout(timeout);
    }

    private void settingNonReliableUDP(int timeout) throws IOException {
        Config.nonReliableSock = new NonReliableSock(Config.clientNonReliablePort, InetAddress.getByName(Config.clientIp), Config.clientNonReliableSegSize, Config.clientNonReliableSleepTime, timeout);
        Config.nonReliableSock.setSoTimeout(timeout);
    }

    @Override
    public void run() {
        Log.d(TAG,"run");
        //configInitialize();

        while (!isStop) {
            connect();

            if(isSet)
                Codec.decode_release();
            Codec.decode_init(Config.videoResizeWidth, Config.videoResizeHeight, Codec.decode_get_h264_identifier());
            Codec.surface_init(viewSurface, Config.videoWidth, Config.videoHeight);
            fileInitialize();

            isSet = true;
            while (!isStop) {
                if(!recvFrame())
                    strangeCount++;
                else
                    strangeCount = 0;

                synchronized (pauseLock) {
                    pauseLock.notify();
                }

                if(isPause) {
                    synchronized (pauseLock) {
                        try {
                            pauseLock.wait();
                        } catch(InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if(strangeCount >= 5) {
                    strangeCount = 0;
                    break;
                }
            }
        }
    }

    public void configInitialize() {
        File configFolder = new File(Config.configPath);

        if(!configFolder.exists())
            configFolder.mkdir();
        else {
            File configFile = new File(Config.configPath+"/config.txt");
            if(configFile.exists()) {
                try {
                    Scanner scanner = new Scanner(configFile);
                    if(scanner.hasNextInt())
                        Config.videoResizeWidth = scanner.nextInt();
                    if(scanner.hasNextInt())
                        Config.videoResizeHeight = scanner.nextInt();
                    if(scanner.hasNextInt())
                        Config.bitRate = scanner.nextInt();
                    if(scanner.hasNextInt())
                        Config.frameRate = scanner.nextInt();
                    if(scanner.hasNextInt())
                        Config.keyFrameRate = scanner.nextInt();
                } catch(FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void configInitialize(int width, int height, int bitRate, int frameRate, int keyFrameRate) {
        if(width != 0)
            Config.videoResizeWidth = width;
        if(height != 0)
            Config.videoResizeHeight = height;
        if(bitRate != 0)
            Config.bitRate = bitRate;
        if(frameRate != 0)
            Config.frameRate = frameRate;
        if(keyFrameRate != 0)
            Config.keyFrameRate = keyFrameRate;
    }

    public boolean sendConfig() {
        byte[] config = new byte[24];
        System.arraycopy(new byte[]{0x12,0x34,0x56,0x00},0,config,0,4);
        System.arraycopy(Config.toBytes(Config.videoResizeWidth),0,config,4,4);
        System.arraycopy(Config.toBytes(Config.videoResizeHeight),0,config,8,4);
        System.arraycopy(Config.toBytes(Config.bitRate),0,config,12,4);
        System.arraycopy(Config.toBytes(Config.frameRate),0, config,16,4);
        System.arraycopy(Config.toBytes(Config.keyFrameRate),0,config,20,4);
        switch (Config.bitRate) {
            case 16384:
                Config.clientNonReliableSleepTime = 60;
                Config.clientReliableSleepTime = 60;
                Config.crf = 47;
                break;
            case 24576:
                Config.clientNonReliableSleepTime = 45;
                Config.clientReliableSleepTime = 45;
                Config.crf = 44;
                break;
            case 32768:
                Config.clientNonReliableSleepTime = 30;
                Config.clientReliableSleepTime = 30;
                Config.crf = 34;
                break;
            case 40960:
                Config.clientNonReliableSleepTime = 26;
                Config.clientReliableSleepTime = 26;
                Config.crf = 39;
                break;
            case 49152:
                Config.clientNonReliableSleepTime = 22;
                Config.clientReliableSleepTime = 22;
                Config.crf = 38;
                break;
            case 57344:
                Config.clientNonReliableSleepTime = 17;
                Config.clientReliableSleepTime = 17;
                Config.crf = 37;
                break;
            case 65536:
                Config.clientNonReliableSleepTime = 15;
                Config.clientReliableSleepTime = 15;
                Config.crf = 35;
                break;
            default:
                Config.crf = 40;
                break;
        }


        boolean result = Config.reliableSock.send(config, Config.serverAddress, Config.serverReliablePort);
        Config.nonReliableSock.clear();
        return result;
    }

    public void connect() {
        Log.d(TAG, "connect");

        int count = 0;
        while(!sendConfig() && !isStop) {
            count++;
            if(count%5 == 0) {
                viewActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(viewActivity, "Please check network connection.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean getIsDrawRect() {
        return isDrawRect;
    }

    public void setIsDrawRect(boolean flag) {
        isDrawRect = flag;
    }

    public boolean recvFrame() {
        //Log.d(TAG, "recvFrame");
        byte[] packetSize = Config.nonReliableSock.recv(Config.clientNonReliableSegSize, Config.serverAddress);

        if(packetSize.length == 0)
            return false;
        else {
            strangeCount = 0;
            if(packetSize[0] == 0x78 && packetSize[1] == 0x56 && packetSize[2] == 0x34 && packetSize[3] == 0x12) {
                int intPacketSize = Config.toInt(packetSize, 4);
                ByteBuffer buf = ByteBuffer.wrap(packetSize);
                LongBuffer longBuf = buf.asLongBuffer();
                long l[] = new long[longBuf.capacity()];
                longBuf.get(l);
                long frameTimestamp = l[1];
                boolean isSave = false;
                if(packetSize[16] == 1)
                    isSave = true;
                byte[] completeFrame;
                if(intPacketSize > packetSize.length) {
                    byte[] frame = Config.nonReliableSock.recv(intPacketSize - packetSize.length, Config.serverAddress);
                    completeFrame = new byte[packetSize.length - 17 + frame.length];
                    System.arraycopy(packetSize, 17, completeFrame, 0, packetSize.length - 17);
                    System.arraycopy(frame, 0, completeFrame, packetSize.length - 17, frame.length);
                }
                else {
                    completeFrame = new byte[packetSize.length - 17];
                    System.arraycopy(packetSize, 17, completeFrame, 0, packetSize.length - 17);
                }

                if (completeFrame.length != 0) {
                    byte[] decodedFrame = Codec.decode_frame(completeFrame);
                    if (isFile)
                        Codec.write_video(decodedFrame);
                    if (decodedFrame.length == 1) {
                        sendConfig();
                        Codec.decode_release();
                        Codec.decode_init(Config.videoResizeWidth, Config.videoResizeHeight, Codec.decode_get_h264_identifier());
                        Codec.surface_init(viewSurface, Config.videoWidth, Config.videoHeight);
                        return false;
                    } else {
                        byte[] result = resizeDecodeImage(decodedFrame, Config.videoResizeWidth, Config.videoResizeHeight, Config.videoWidth, Config.videoHeight);
                        if(isSave == true) {
                            saveToJpg(result, Config.videoWidth, Config.videoHeight, frameTimestamp);
                            timestamps.add(frameTimestamp);
                            if(timestamps.size() == Config.maxNode) {
                                File firstSNImg = new File(Config.frPath + timestamps.get(0) + "-SN.jpg");
                                File firstOrImg = new File(Config.frPath + timestamps.get(0) + "-original.jpg");
                                boolean deleted = firstSNImg.delete();
                                boolean deleted2 = firstOrImg.delete();

                                if (!deleted)
                                    System.out.println("file delete error occured!!");

                                if (!deleted2)
                                    System.out.println("file delete error occured!!");

                                timestamps.remove(0);
                            }
                        }
                        if(isDrawRect) {
                            byte[] rectResult = drawRect(result, Config.videoWidth, Config.videoHeight, currentRectList);
                            rectClear++;
                            if (rectClear >= Config.frameRate * 2)
                                currentRectList = null;
                            Codec.draw_to_surface(rectResult);
                        }
                        else
                            Codec.draw_to_surface(result);
                    }
                }
            }
            else if(packetSize[0] == 0x78 && packetSize[1] == 0x56 && packetSize[2] == 0x34 && packetSize[3] == 0x11) {
                List<Rect> rectList = new ArrayList<>();
                short number = Config.toShort(packetSize, 4);
                byte[] result = new byte[number*8];
                System.arraycopy(packetSize, 6, result, 0, packetSize.length - 6);
                Log.d(TAG, number+"");
                if(6 + number*8 > packetSize.length) {
                    byte[] remain = Config.nonReliableSock.recv(6 + number*8 - packetSize.length, Config.serverAddress);
                    System.arraycopy(remain, 0, result, packetSize.length - 6, remain.length);
                }

                for(int i=0; i<number; i++) {
                    rectList.add(new Rect(new Point((double)Config.toShort(result, i*8), (double)Config.toShort(result, 2+i*8)),
                            new Point((double)Config.toShort(result, 4+i*8), (double)Config.toShort(result, 6+i*8))));
                }
                currentRectList = rectList;
                rectClear = 0;
            }

        }
        return true;
    }

    private byte[] resizeDecodeImage(byte[] data, int srcWidth, int srcHeight, int dstWidth, int dstHeight) {
        Mat mat = new Mat(srcHeight*3/2, srcWidth, CvType.CV_8UC1);
        mat.put(0, 0, data);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_YUV2RGBA_YV12);
        //Imgproc.resize(mat, mat, new Size(dstWidth, dstHeight));
        Imgproc.resize(mat, mat, new Size(), ((double)dstWidth)/srcWidth, ((double)dstHeight)/srcHeight, Imgproc.INTER_CUBIC);

        byte[] result =  new byte[dstWidth*dstHeight*4];
        mat.get(0, 0, result);

        return result;
    }

    private byte[] drawRect(byte[] data, int width, int height, List<Rect> rectList) {
        Mat mat = new Mat(height, width, CvType.CV_8UC4);
        mat.put(0, 0, data);
        if(rectList != null) {
            for (int i = 0; i < rectList.size(); i++)
                Core.rectangle(mat, rectList.get(i).tl(), rectList.get(i).br(), new Scalar(255, 0, 0));
        }

        byte[] result =  new byte[width*height*4];
        mat.get(0, 0, result);

        return result;
    }

    private void saveToJpg(byte[] data, int width, int height, long timestamp) {
        Log.d(TAG, "saveToJpg");
        ByteBuffer byteBuf = ByteBuffer.wrap(data);
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.copyPixelsFromBuffer(byteBuf);
        File originalImg = new File(Config.frPath+timestamp+"-original.jpg");
        try {
            final FileOutputStream filestream = new FileOutputStream(originalImg);
            bmp.compress(Bitmap.CompressFormat.JPEG, 70, filestream);
        } catch(FileNotFoundException e) {
            System.out.println("save To Jpg FileNotFoundException");
            e.printStackTrace();
        }

        Bitmap resizedBmp = Bitmap.createScaledBitmap(bmp, 256, 256, false);
        File resizedImg = new File(Config.frPath+timestamp+"-SN.jpg");
        try {
            final FileOutputStream filestream = new FileOutputStream(resizedImg);
            resizedBmp.compress(Bitmap.CompressFormat.JPEG, 70, filestream);
        } catch(FileNotFoundException e) {
            System.out.println("save To Jpg FileNotFoundException");
            e.printStackTrace();
        }
    }

    public void fileInitialize() {
        Log.d(TAG, "fileInitialize");
        File file = new File(Config.frPath);

        if(file.exists()) {
            if (file.isDirectory()) {
                String[] children = file.list();
                for (int i = 0; i < children.length; i++)
                    new File(file, children[i]).delete();
            }
            file.delete();
        }

        if(!file.exists())
            file.mkdir();
    }

    public ArrayList<Long> getTimestampList() {
        Config.reliableSock.clear();

        ArrayList<Long> timestamps = new ArrayList<>();
        byte[] requestTsListData = new byte[]{0x34, 0x56, 0x78, 0x12, 0x01};
        if(!Config.reliableSock.send(requestTsListData, Config.serverAddress, Config.serverReliablePort))
            return null;

        byte[] timestampSize = Config.reliableSock.recv(4, Config.serverAddress, Config.serverReliablePort);
        if(timestampSize.length != 0) {
            byte[] timestampList = Config.reliableSock.recv(8 * Config.toInt(timestampSize, 0), Config.serverAddress, Config.serverReliablePort);
            if(timestampList.length != 0) {
                ByteBuffer buf = ByteBuffer.wrap(timestampList);
                LongBuffer longBuf = buf.asLongBuffer();
                long l[] = new long[longBuf.capacity()];
                longBuf.get(l);
                for (int j = 0; j < Config.toInt(timestampSize, 0); j++) {
                    Log.d(TAG, "timestamp " + j + " : " + l[j]);
                    timestamps.add(l[j]);
                }

                Collections.sort(timestamps, new Comparator<Long>() {
                    @Override
                    public int compare(Long aLong, Long t1) {
                        return aLong.compareTo(t1);
                    }
                });
            }
        }
        else
            return null;

        return timestamps;
    }

    public byte[] requestImg(long timestamp, int preX, int preY, int postX, int postY) {
        Config.reliableSock.clear();

        byte[] requestImgData = new byte[]{0x34, 0x56, 0x78, 0x12, 0x02};
        if(!Config.reliableSock.send(requestImgData, Config.serverAddress, Config.serverReliablePort))
            return null;

        byte[] rect = new byte[24];
        Log.d(TAG, "prex: "+preX+" prey: "+preY+" postX: "+postX+" postY: "+postY);
        System.arraycopy(Config.LongtoBytes(timestamp), 0, rect, 0, 8);
        System.arraycopy(Config.toBytes(preX), 0, rect, 8, 4);
        System.arraycopy(Config.toBytes(preY), 0, rect, 12, 4);
        System.arraycopy(Config.toBytes(postX), 0, rect, 16, 4);
        System.arraycopy(Config.toBytes(postY), 0, rect, 20, 4);
        if(!Config.reliableSock.send(rect, Config.serverAddress, Config.serverReliablePort))
            return null;

        byte[] rawDataSize = Config.reliableSock.recv(4, Config.serverAddress, Config.serverReliablePort);
        if(rawDataSize.length != 0) {
            Log.d(TAG, "rawData SIze is : " + Config.toInt(rawDataSize, 0));
            if (Config.toInt(rawDataSize, 0) >= 100000)
                return null;
            else {
                //System.out.println("jpeg Size is "+ Config.toInt(rawDataSize, 0));
                byte[] wantedImg = Config.reliableSock.recv(Config.toInt(rawDataSize, 0), Config.serverAddress, Config.serverReliablePort);
                if (wantedImg.length != 0)
                    return wantedImg;
            }
        }
        return null;
    }

    public boolean reStart(boolean IsReset) {
        Log.d(TAG, "reStart");

        isPause = false;
        boolean result = true;
        if(IsReset) {
            do {
                result = sendConfig();
            } while(!result);
        }
        synchronized (pauseLock) {
            pauseLock.notify();
        }

        return result;
    }

    public boolean pause() {
        Log.d(TAG, "pause");
        isPause = true;
        synchronized (pauseLock) {
            try {
                pauseLock.wait();
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }

        byte[] pause = new byte[]{0x12, 0x34, 0x56, 0x01};
        return Config.reliableSock.send(pause, Config.serverAddress, Config.serverReliablePort);
    }

    public void saveConfig() {
        File configFolder = new File(Config.configPath);
        if(!configFolder.exists())
            configFolder.mkdir();

        File configFile = new File(Config.configPath+"/config.txt");
        if(!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileWriter writer = new FileWriter(configFile);
            writer.write("");
            writer.append(Config.videoResizeWidth+"\n");
            writer.append(Config.videoResizeHeight+"\n");
            writer.append(Config.bitRate+"\n");
            writer.append(Config.frameRate+"\n");
            writer.append(Config.keyFrameRate+"");
            writer.flush();
            writer.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        File file = new File(Config.filePath);
        if(!file.exists())
            file.mkdir();

        Codec.open_file(Config.filePath+"/"+System.currentTimeMillis()+".avi", Config.videoResizeWidth, Config.videoResizeHeight, Config.frameRate, Config.keyFrameRate, 0, Config.bitRate, Config.crf, Codec.decode_get_h264_identifier());
        isFile = true;
    }

    public void saveStop() {
        isFile = false;
        Codec.close_file();
    }

    public void release() {
        Log.d(TAG, "release!!");
        isSet = false;
        isStop = true;
        isPause = false;

        synchronized (pauseLock) {
            pauseLock.notify();
        }

        closeReliableUDP();
        closeNonReliableUDP();
        Codec.decode_release();
        Codec.close_file();
    }

    public void closeReliableUDP() {
        if(Config.reliableSock != null) {
            Config.reliableSock.close();
            Config.reliableSock = null;
        }
    }

    public void closeNonReliableUDP() {
        if(Config.nonReliableSock != null) {
            Config.nonReliableSock.close();
            Config.nonReliableSock = null;
        }
    }
}