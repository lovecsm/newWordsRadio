package com.word.radio.newradio;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FloatView extends View {
    private final static String TAG = "FloatView";

    private Context mContext;
    private WindowManager wm;
    private static WindowManager.LayoutParams wmParams;
    public LinearLayout mContentView;
    private float mRelativeX;
    private float mRelativeY;
    private float mScreenX;
    private float mScreenY;
    private boolean bShow = false;
    public TextView textView;

    public FloatView(Context context) {
        super(context);
        wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wmParams == null) {
            wmParams = new WindowManager.LayoutParams();
        }
        mContext = context;
    }


    @SuppressLint("ClickableViewAccessibility")
    public void setLayout(int layout_id) {

        mContentView = (LinearLayout) LayoutInflater.from(mContext).inflate(layout_id, null);
        textView = new TextView(mContentView.getContext());

        textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams reLayoutParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        mContentView.removeView(textView);
        reLayoutParams.setMargins(8, 2, 8, 2);
        textView.setPadding(8, 2, 8, 2);
        textView.setTextColor(Color.BLACK);
        textView.setText(mContext.getString(R.string.hello) + "  " + mContext.getString(R.string.world));
        mContentView.addView(textView, reLayoutParams);


        mContentView.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mScreenX = event.getRawX();
                mScreenY = event.getRawY();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mRelativeX = event.getX();
                        mRelativeY = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (abs(event.getX() - mRelativeX) > 5 || abs(event.getY() - mRelativeY) > 5)
                            updateViewPosition();
                        break;
                    case MotionEvent.ACTION_UP:
                        if (abs(event.getX() - mRelativeX) > 1 || abs(event.getY() - mRelativeY) > 1)
                            ;
                            //updateViewPosition();
                        else {
                            Intent it = new Intent("ok");
                            mContext.sendBroadcast(it);
                        }
                        mRelativeX = mRelativeY = 0;
                        break;
                }
                return true;
            }
        });
    }

    private void updateViewPosition() {
        if (mScreenX - mRelativeX > 5 || mScreenY - mRelativeY > 5) {
            wmParams.x = (int) (mScreenX - mRelativeX);
            wmParams.y = (int) (mScreenY - mRelativeY - 60);
            wm.updateViewLayout(mContentView, wmParams);
        }
    }

    public void show() {
        if (mContentView != null) {
            wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            wmParams.format = PixelFormat.RGBA_8888;
            wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            wmParams.alpha = 0.6f;
            wmParams.gravity = Gravity.START | Gravity.TOP;
            wmParams.x = getWidth() / 3;
            wmParams.y = 0;
            wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            // 显示自定义悬浮窗口
            wm.addView(mContentView, wmParams);
            bShow = true;
        }
    }

    public void close() {
        if (mContentView != null && mContentView.getParent() != null) {
            wm.removeView(mContentView);
            bShow = false;
        }
        textView = null;
    }

    public boolean isShow() {
        return bShow;
    }

    private float abs(float a) {
        return a > 0 ? a : -a;
    }
}

