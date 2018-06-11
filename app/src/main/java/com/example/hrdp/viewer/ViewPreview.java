package com.example.hrdp.viewer;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

/**
 * Created by Mansu on 2016-06-12.
 */
public class ViewPreview extends SurfaceView implements SurfaceHolder.Callback {
    private String TAG = "ViewPreview";
    private SurfaceHolder mHolder = null;
    private ViewThread viewThread;
    private boolean previewIsRunning = false;
    private boolean isSurfaceCreated = false;

    public ViewPreview(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
        viewThread = ViewThread.getInstance();
    }

    public void surfaceCreated(SurfaceHolder holder) {
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        mHolder = holder;
        isSurfaceCreated = true;
        myStartPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        myStopPreview();
    }

    // safe call to start the preview
    // if this is called in onResume, the surface might not have been created yet
    // so check that the camera has been set up too.
    public void myStartPreview() {
        Log.d(TAG, "myStartPreview " + previewIsRunning);
        if(!previewIsRunning && isSurfaceCreated) {
            previewIsRunning = true;
            //viewThread에서 surface에 직접접근 하므로 surface를 받는것이 필요하다.
            viewThread.setSurface(mHolder.getSurface());
            if(!viewThread.isAlive())
                viewThread.start();
        }
    }

    public void myStopPreview() {
        Log.d(TAG, "myStopPreview " + previewIsRunning);
        if (previewIsRunning) {
            previewIsRunning = false;
            isSurfaceCreated = false;
        }
    }
}