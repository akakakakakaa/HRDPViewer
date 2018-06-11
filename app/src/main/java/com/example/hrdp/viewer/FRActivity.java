package com.example.hrdp.viewer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.evs.viewer.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Created by yj on 16. 8. 30.
 */
public class FRActivity extends Activity {
    private String TAG = "FRActivity";
    private Context frContext = null;
    private FRClient frClient = null;
    private ProgressDialog dialog = null;
    private long currentImgTS = 0;
    private ViewThread viewThread = null;
    private int strangeCount = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewThread = ViewThread.getInstance();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.view_fractivity);

        findViewById(R.id.frBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        dialog = ProgressDialog.show(this, "Loading", "Please wait...", true);
        frContext = this;
    }

    @Override
    public void onResume() {
        super.onResume();
        dialog.show();
        System.out.println("FRActivity onResume start");
        if (frClient == null) {
            frClient = new FRClient();
            frClient.start();
        }
        System.out.println("FRActivity onResume end");
    }

    public void addImages(List<ImageInfo> imageList) {
        TouchEvent touchEvent = new TouchEvent(this);
        FrameLayout canvasLayout = (FrameLayout) findViewById(R.id.canvasLayout);
        canvasLayout.addView(touchEvent);
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayout);

        Collections.sort(imageList, new Comparator<ImageInfo>() {
            @Override
            public int compare(ImageInfo aLong, ImageInfo t1) {
                return (aLong.getDate().compareTo(t1.getDate()));
            }
        });

        for (ImageInfo imageUrl : imageList) {
            imageUrl.setTouchEvent(touchEvent);
            float width =TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics());
            float height =TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics());
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((int)width, (int)height);
            layoutParams.setMargins(0, 0, 15, 0);

            LinearLayout innerLinearLayout = new LinearLayout(this);

            LinearLayout.LayoutParams viewLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            viewLayoutParams.gravity = Gravity.BOTTOM;
            TextView textView = new TextView(innerLinearLayout.getContext());
            textView.setLayoutParams(viewLayoutParams);

            textView.setTextColor(Color.WHITE);
            textView.setBackgroundColor(Color.parseColor("#88D5D5D5"));
            textView.setText(imageUrl.getDate());

            innerLinearLayout.setBackgroundDrawable(new BitmapDrawable(BitmapFactory.decodeFile(imageUrl.getImagePath())));   // 수정필요
            innerLinearLayout.setLayoutParams(layoutParams);
            innerLinearLayout.setOnClickListener(imageUrl.getOnClick());
            innerLinearLayout.addView(textView);

            linearLayout.addView(innerLinearLayout);
        }

        String path = imageList.get(0).getImagePath();
        ImageView topImageView = (ImageView) findViewById(R.id.topImageView);
        topImageView.setBackgroundDrawable(new BitmapDrawable(BitmapFactory.decodeFile(path.substring(0, path.indexOf("-")) + "-original.jpg")));   // 수정필요
        currentImgTS = imageList.get(0).getTimestamp();

        dialog.dismiss();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                finish();
            }
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (frClient != null) {
            frClient.release();
            frClient = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    public class FRClient extends Thread {
        public FRClient() {
        }

        public void run() {
            strangeCount = 0;
            while(true) {
                ArrayList<Long> timestamps = viewThread.getTimestampList();
                if (timestamps != null) {
                    List<ImageInfo> imageList = new ArrayList<>();
                    File f = new File(Config.frPath);
                    if (!f.exists())
                        f.mkdir();
                    File file[] = f.listFiles();
                    for (int i = 0; i < file.length; i++)
                        if (file[i].getName().indexOf("-SN.jpg") != -1) {
                            long timestamp = Long.parseLong(file[i].getName().substring(0, file[i].getName().indexOf("-")));
                            int index = timestamps.indexOf(timestamp);
                            if(index != -1) {
                                ImageInfo imageInfo = new ImageInfo(Config.frPath + file[i].getName(), timestamps.get(index)); // 수정 필요
                                imageList.add(imageInfo);
                            }
                        }

                    if(imageList.size() != 0) {
                        final List<ImageInfo> result = imageList;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                addImages(result);
                            }
                        });
                    }
                    else
                        strangeCount = 10;

                    break;
                } else
                    strangeCount++;

                if(strangeCount >= 10)
                    break;
            }

            if(strangeCount >= 10)
                finish();
        }

        public void release() {
        }
    }

    public class ImageInfo {
        // Bitmap image
        private String path;
        private long timestamp;
        private String date;
        private TouchEvent event;
        // 이미지 클릭시 실행할 OnClickListener
        private FrameLayout.OnClickListener onClick;

        /**
         * Constructor
         *
         * @param path
         */
        public ImageInfo(String path, long time) {
            this.path = path;
            timestamp = time;
            Calendar cal = Calendar.getInstance(Locale.KOREA);
            cal.setTimeInMillis(time);
            date = DateFormat.format("hh:mm:ss", cal).toString();
            onClick = onClickListener;
        }

        public void setTouchEvent(TouchEvent m_event) {
            event = m_event;
        }

        public View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TEST
                System.out.println("onClick!!");
                ImageView topImageView = (ImageView) findViewById(R.id.topImageView);
                topImageView.setBackgroundDrawable(new BitmapDrawable(BitmapFactory.decodeFile(path.substring(0, path.indexOf("-")) + "-original.jpg")));   // 수정필요
                currentImgTS = timestamp;
                event.clear();
            }
        };

        public String getImagePath() {
            return path;
        }

        public String getDate() {
            return date;
        }

        public long getTimestamp() { return timestamp; }

        public RelativeLayout.OnClickListener getOnClick() {
            return onClick;
        }
    }

    class TouchEvent extends View {

        private float prex;
        private float prey;
        private float postx;
        private float posty;
        private Paint paint;
        private boolean isDrawing;
        private Bitmap sub = null;
        private boolean isSub;

        public TouchEvent(Context context) {
            super(context);
            initialize();
        }

        public void initialize() {
            this.prex = 0;
            this.prey = 0;
            this.postx = 0;
            this.posty = 0;

            this.isDrawing = false;

            paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            paint.setColor(Color.RED);
        }

        public void clear() {
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (isDrawing) {
                //if(posty)
                canvas.drawRect(prex, prey, postx, posty, paint);
            }
            if (isSub == true) {
                float leftX = prex > postx ? postx : prex;
                float leftY = prey > posty ? posty : prey;
                canvas.drawBitmap(sub, leftX, leftY, paint);
                isSub = false;
                sub = null;
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getAction();

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    prex = event.getX();
                    prey = event.getY();
                    postx = prex + 1;
                    posty = prey + 1;
                    isDrawing = true;
                    break;
                case MotionEvent.ACTION_MOVE:
                    postx = event.getX();
                    posty = event.getY();
                    break;
                case MotionEvent.ACTION_UP:
                    isDrawing = false;
                    performClick();
                    dialog.show();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if(prex < 1)
                                prex = 1;
                            if(postx < 1)
                                postx = 1;
                            if(prey < 1)
                                prey = 1;
                            if(posty < 1)
                                posty = 1;
                            if(prex > Config.videoWidth - 1)
                                prex = Config.videoWidth - 1;
                            if(postx > Config.videoWidth - 1)
                                postx = Config.videoWidth - 1;
                            if(prey > Config.videoHeight - 1)
                                prey = Config.videoHeight - 1;
                            if(posty > Config.videoHeight - 1)
                                posty = Config.videoHeight - 1;

                            byte[] base64Jpg = viewThread.requestImg(currentImgTS,
                                    (int)prex, (int)prey, (int)postx, (int)posty);
                            if(base64Jpg != null) {
                                byte[] jpg = Base64.decode(base64Jpg, Base64.DEFAULT);
                                try {
                                    File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/tmp.jpg");
                                    FileOutputStream fout = new FileOutputStream(file);
                                    fout.write(jpg);
                                    fout.flush();
                                    fout.close();
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                sub = BitmapFactory.decodeByteArray(jpg, 0, jpg.length);
                                if(sub != null) {
                                    //sub = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().getAbsolutePath()+"/tmp.jpg");
                                    isSub = true;
                                }
                                else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(FRActivity.this, "Invalid JPG Data", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                            }
                            else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(FRActivity.this, "Access fail", Toast.LENGTH_SHORT).show();
                                    }
                                });

                            }
                                //base64jpg exception
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    invalidate();
                                }
                            });
                            dialog.dismiss();
                        }
                    }).start();
                    break;
            }
            invalidate();
            return true;
        }
        float convertPixelsToDp(float px, Context context){
            Resources resources = context.getResources();
            DisplayMetrics metrics = resources.getDisplayMetrics();
            float dp = px / (metrics.densityDpi / 160f);
            return dp;
        }
    }
}