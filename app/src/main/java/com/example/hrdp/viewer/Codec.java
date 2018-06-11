package com.example.hrdp.viewer;

import android.view.Surface;

/**
 * Created by yj on 16. 7. 29.
 */
public class Codec {
    static {
        System.loadLibrary("ndkTest");
    }

    static public native int decode_get_h264_identifier();
    static public native int decode_get_h265_identifier();
    static public native void decode_init(int width, int height, int codec_type);
    static public native void surface_init(Surface surface, int width, int height);
    static public native byte[] decode_frame(byte[] data);
    static public native void decode_release();
    static public native void draw_to_surface(byte[] data);
    static public native void open_file(String filename, int width, int height, int frame_rate, int key_frame_rate, int color_format, int bit_rate, int crf, int codec_type);
    static public native void write_video(byte[] buf);
    static public native void close_file();
}