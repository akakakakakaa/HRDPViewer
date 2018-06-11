package com.example.hrdp.viewer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.evs.viewer.R;

import java.util.ArrayList;

/**
 * Created by yj on 16. 8. 31.
 */
public class OptionPopupWindow {
    private View optionView;
    private PopupWindow popupWindow = null;
    private ListView optionList;
    private Button applyBtn;
    private Button cancelBtn;

    private int changedbitRate = 0;
    private int changedFrameRate = 0;
    private int changedKeyFrameRate = 0;
    private int changedVideoWidth = 0;
    private int changedVideoHeight = 0;

    public void show(final Activity activity) {
        if(popupWindow == null) {
            optionView = activity.getLayoutInflater().inflate(R.layout.popup_option, null);

            optionList = (ListView) optionView.findViewById(R.id.optionList);
            ArrayList<OptionData> data = new ArrayList<>();

            ArrayList<String> bitrates = new ArrayList<>();
            bitrates.add("16");
            bitrates.add("24");
            bitrates.add("32");
            bitrates.add("40");
            bitrates.add("48");
            bitrates.add("56");
            bitrates.add("64");
            data.add(new OptionData("대역폭", Integer.toString(Config.bitRate / 1024), bitrates));

            ArrayList<String> frames = new ArrayList<>();
            frames.add("1");
            frames.add("2");
            frames.add("3");
            frames.add("5");
            frames.add("7");
            frames.add("10");
            data.add(new OptionData("초당 프레임 수", Integer.toString(Config.frameRate), frames));

            ArrayList<String> resolutions = new ArrayList<>();
            resolutions.add("160x120");
            resolutions.add("320x240");
            data.add(new OptionData("해상도", Integer.toString(Config.videoResizeWidth) + "x" + Integer.toString(Config.videoResizeHeight), resolutions));

            CustomAdapter adapter = new CustomAdapter(activity, data);
            optionList.setAdapter(adapter);

            popupWindow = new PopupWindow(optionView, RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

            applyBtn = (Button) optionView.findViewById(R.id.applyBtn);
            cancelBtn = (Button) optionView.findViewById(R.id.cancelBtn);

            applyBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(changedbitRate != 0 || changedFrameRate != 0 || changedKeyFrameRate != 0 || changedVideoWidth != 0 || changedVideoHeight != 0) {
                        new AsyncTask<Void, Void, Boolean>() {
                            @Override
                            protected void onPreExecute() {
                                super.onPreExecute();
                                ViewActivity.dialog.setMessage("준비중입니다.");
                                ViewActivity.dialog.show();
                            }

                            @Override
                            protected Boolean doInBackground(Void... voids) {
                                boolean result = ViewActivity.viewThread.pause();
                                if(result) {
                                    ViewActivity.viewThread.saveConfig();
                                    ViewActivity.viewThread.configInitialize(changedVideoWidth, changedVideoHeight, changedbitRate, changedFrameRate, changedKeyFrameRate);
                                }
                                ViewActivity.viewThread.reStart(true);

                                return result;
                            }

                            @Override
                            protected void onPostExecute(Boolean result) {
                                super.onPostExecute(result);
                                if(!result)
                                    Toast.makeText(activity, "서버와 연결이 원활하지 않습니다.", Toast.LENGTH_LONG).show();
                                ViewActivity.dialog.dismiss();
                            }
                        }.execute();
                    }
                    dismiss();
                }
            });
            cancelBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismiss();
                }
            });

            popupWindow.setOutsideTouchable(true);
            popupWindow.setFocusable(true);
            popupWindow.setBackgroundDrawable(new BitmapDrawable());
            popupWindow.showAtLocation(optionView, Gravity.CENTER, 0, 0);
        }
    }

    public void dismiss() {
        if(popupWindow != null) {
            popupWindow.dismiss();
            popupWindow = null;
        }
    }

    private class CustomAdapter extends BaseAdapter {
        private LayoutInflater inflater;
        private ArrayList<OptionData> data;

        public CustomAdapter(Context context, ArrayList<OptionData> data) {
            this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.data = data;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int i) {
            return data.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {
            if(view == null)
                view = inflater.inflate(R.layout.view_option_item, viewGroup, false);

            TextView optionName = (TextView)view.findViewById(R.id.option_name);
            final TextView optionItem = (TextView)view.findViewById(R.id.option_item);
            Button leftBtn = (Button)view.findViewById(R.id.option_left);
            Button rightBtn = (Button)view.findViewById(R.id.option_right);

            final OptionData option = data.get(i);

            optionName.setText(option.name);
            optionItem.setText(option.options.get(option.prevIdx));
            leftBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(option.curIdx == 0)
                        option.curIdx = option.options.size() - 1;
                    else
                        option.curIdx--;

                    optionItem.setText(option.options.get(option.curIdx));
                    if(option.curIdx != option.prevIdx) {
                        optionItem.setTextColor(Color.parseColor("#FF0000"));
                        update(i, option.options.get(option.curIdx));
                    }
                    else {
                        optionItem.setTextColor(Color.parseColor("#FFFFFF"));
                        clear(i);
                    }
                }
            });

            rightBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(option.curIdx == option.options.size() - 1)
                        option.curIdx = 0;
                    else
                        option.curIdx++;

                    optionItem.setText(option.options.get(option.curIdx));
                    if(option.curIdx != option.prevIdx) {
                        optionItem.setTextColor(Color.parseColor("#FF0000"));
                        update(i, option.options.get(option.curIdx));
                    }
                    else {
                        optionItem.setTextColor(Color.parseColor("#FFFFFF"));
                        clear(i);
                    }
                }
            });

            return view;
        }

        private void update(int i, String data) {
            switch(i) {
                case 0:
                    changedbitRate = Integer.parseInt(data)*1024;
                    break;
                case 1:
                    changedFrameRate = Integer.parseInt(data);
                    changedKeyFrameRate = Integer.parseInt(data)*2;
                    break;
                case 2:
                    changedVideoWidth = Integer.parseInt(data.substring(0, data.indexOf("x")));
                    changedVideoHeight = Integer.parseInt(data.substring(data.indexOf("x")+1, data.length()));
                    break;
            }
        }

        public void clear(int i) {
            switch(i) {
                case 0:
                    changedbitRate = 0;
                    break;
                case 1:
                    changedFrameRate = 0;
                    changedKeyFrameRate = 0;
                    break;
                case 2:
                    changedVideoWidth = 0;
                    changedVideoHeight = 0;
                    break;
            }
        }
    };

    private class OptionData {
        public String name;
        public ArrayList<String> options;
        public int curIdx;
        public int prevIdx;

        public OptionData(String name, String currentOption, ArrayList<String> options) {
            this.name = name;
            this.options = options;

            prevIdx = options.indexOf(currentOption);
            if(prevIdx == -1)
                prevIdx = 0;
            curIdx = prevIdx;
        }
    };
}