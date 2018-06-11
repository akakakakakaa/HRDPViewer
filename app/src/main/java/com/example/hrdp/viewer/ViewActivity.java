package com.example.hrdp.viewer;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.evs.viewer.R;

public class ViewActivity extends AppCompatActivity {
    public static final String ACTION_CLOSE = "com.evs.viewer.ACTION_CLOSE"; // 종료 브로드캐스트 액션
    public static final String TAG = "ViewActivity";
    public static ViewThread viewThread = null;

    private ViewPreview viewPreview = null;
    private OptionPopupWindow optionPopupWindow = new OptionPopupWindow();
    public static ProgressDialog dialog;
    private boolean saveFlag = true;
    private CloseBroadcastReceiver mCloseReceiver; // 종료 브로드캐스트 수신 클래스
    private class CloseBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_CLOSE)) {
                ViewActivity.this.finish();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.view_frame);

        setCloseBroadcastReceiver();
        setProgressDialog();
        setViewThread();
        setViewPreview();
        setBtnEvent();
    }

    private void setCloseBroadcastReceiver() {
        //set broadcastreceiver for closing
        IntentFilter filter = new IntentFilter(ACTION_CLOSE);
        mCloseReceiver = new CloseBroadcastReceiver();
        registerReceiver(mCloseReceiver, filter);
    }

    private void setProgressDialog() {
        //set Dialog
        //parameter에서 new progressdialog하면 nullpointerexception뜸;
        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCanceledOnTouchOutside(false);
    }

    private void setViewThread() {
        viewThread = ViewThread.getInstance();
        viewThread.setView(this);
    }

    private void setViewPreview() {
        //set Preview

        viewPreview = new ViewPreview(this);
        FrameLayout viewFrameLayout = (FrameLayout)findViewById(R.id.surfaceFrame);
        viewFrameLayout.addView(viewPreview);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(Config.videoWidth, Config.videoHeight);
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        viewFrameLayout.setLayoutParams(params);
    }

    private void setBtnEvent() {
        //set Button Event
        final Button detectBtn = (Button)findViewById(R.id.detectBtn);
        final Button saveBtn = (Button)findViewById(R.id.saveBtn);
        final Button optBtn = (Button)findViewById(R.id.optBtn);
        final Button frBtn = (Button)findViewById(R.id.frBtn);
        final Button fileManagerBtn = (Button)findViewById(R.id.fileManagerBtn);

        detectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(viewThread.getIsDrawRect()) {
                    detectBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.icon_detect));
                    viewThread.setIsDrawRect(false);
                }
                else {
                    detectBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.stop));
                    viewThread.setIsDrawRect(true);
                }
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(saveFlag) {
                    Log.d(TAG, "SaveStart");
                    viewThread.save();
                    //Toast.makeText(viewContext, "save start", Toast.LENGTH_LONG);
                    saveBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.stop));
                    optBtn.setEnabled(false);
                    frBtn.setEnabled(false);
                    fileManagerBtn.setEnabled(false);
                    saveFlag = false;
                }
                else {
                    Log.d(TAG, "SaveStop");
                    viewThread.saveStop();
                    saveBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.save));
                    optBtn.setEnabled(true);
                    frBtn.setEnabled(true);
                    fileManagerBtn.setEnabled(true);
                    saveFlag = true;
                }
            }
        });

        optBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "optionPopupWindow");
                optionPopupWindow.show(ViewActivity.this);
            }
        });

        frBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        dialog.setMessage("준비중입니다.");
                        dialog.show();
                    }

                    @Override
                    protected Boolean doInBackground(Void... voids) {
                        return viewThread.pause();
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        super.onPostExecute(result);
                        if(result) {
                            Log.d(TAG, "FRSuccess");
                            Intent intent = new Intent(ViewActivity.this, FRActivity.class);
                            startActivityForResult(intent, 2);
                        }
                        else {
                            Toast.makeText(ViewActivity.this, "서버와 연결이 원할하지 않습니다.", Toast.LENGTH_LONG).show();
                            viewThread.reStart(false);
                        }
                        dialog.dismiss();
                    }
                }.execute();
            }
        });

        fileManagerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        dialog.setMessage("준비중입니다.");
                        dialog.show();
                    }

                    @Override
                    protected Boolean doInBackground(Void... voids) {
                        return viewThread.pause();
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        super.onPostExecute(result);
                        if(result) {
                            Log.d(TAG, "FMSuccess");
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setDataAndType(Uri.parse("file:/" + Config.filePath), "video/*");
                            startActivityForResult(intent, 3);
                        }
                        else {
                            Toast.makeText(ViewActivity.this, "서버와 연결이 원할하지 않습니다.", Toast.LENGTH_LONG).show();
                            viewThread.reStart(false);
                        }
                        dialog.dismiss();
                    }
                }.execute();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume!!");
        viewPreview.myStartPreview();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        viewPreview.myStopPreview();
    }

    //change activity call onStop
    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        unregisterReceiver(mCloseReceiver);
        if(viewPreview != null) {
            viewThread.release();
            viewPreview = null;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if(event.getAction() == KeyEvent.ACTION_DOWN) {
                if(optionPopupWindow != null) {
                    optionPopupWindow.dismiss();
                    optionPopupWindow = null;
                }
                else
                    finish();
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        Log.d(TAG, "onActivityResult");
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(requestCode == 2)
                    viewThread.reStart(true);
                else if(requestCode == 3) {
                    if(resultCode == RESULT_OK) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        Uri uri = data.getData();
                        intent.setDataAndType(uri, "video/*");
                        startActivityForResult(intent, 4);
                    }
                    else
                        viewThread.reStart(true);
                }
                else if(requestCode == 4)
                    viewThread.reStart(true);
            }
        }).start();
    }
}