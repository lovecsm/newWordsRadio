package com.word.radio.newradio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import com.word.radio.utils.LogUtils;

public class MediaButtonReceiver extends BroadcastReceiver {

    private static String TAG = "MediaButtonReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        // 获得Action
        String intentAction = intent.getAction();
        // 获得KeyEvent对象
        KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        // 按下 / 松开 按钮
        int keyAction = keyEvent.getAction();

        if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)
                && (KeyEvent.ACTION_DOWN == keyAction)) {
            // 获得按键字节码
            int keyCode = keyEvent.getKeyCode();
            //LogUtils.i("keycode", String.valueOf(keyCode));
            // 获得事件的时间
//            downtime = keyEvent.getDownTime();
            // 获取按键码 keyCode
//			StringBuilder sb = new StringBuilder();
//			// 这些都是可能的按键码 ， 打印出来用户按下的键
//			if (KeyEvent.KEYCODE_MEDIA_NEXT == keyCode) {
//				sb.append("KEYCODE_MEDIA_NEXT");
//			}
            // 说明：当我们按下MEDIA_BUTTON中间按钮时，实际出发的是 KEYCODE_HEADSETHOOK 而不是
            // KEYCODE_MEDIA_PLAY_PAUSE
            if (KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE == keyCode) {
                mySendBroadcast(context, "ok");
            }
            if (KeyEvent.KEYCODE_HEADSETHOOK == keyCode || 127 == keyCode || 126 == keyCode) {
                mySendBroadcast(context, "ok");
            }
            if (KeyEvent.KEYCODE_MEDIA_PREVIOUS == keyCode) {
                // previous
                LogUtils.i(TAG, "previous play");
                mySendBroadcast(context, "previous");
            }
            if (KeyEvent.KEYCODE_MEDIA_NEXT == keyCode) {
                // next
                LogUtils.i(TAG, "next play");
                mySendBroadcast(context, "next");
            }
            if (KeyEvent.KEYCODE_MEDIA_STOP == keyCode) {
//				sb.append("KEYCODE_MEDIA_STOP");
                mySendBroadcast(context, "ok");
            }
        } else if (KeyEvent.ACTION_UP == keyAction) {
            if (keyEvent.getEventTime() - keyEvent.getDownTime() > 100) {
                mySendBroadcast(context, "restart");
            }
        }
    }

    private void mySendBroadcast(Context mContext, String action) {
        Intent intent = new Intent(action);
        mContext.sendBroadcast(intent);
    }
}
