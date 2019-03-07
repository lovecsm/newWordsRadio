package com.word.radio.time;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class TimeChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null)
            Log.i("TimeBroadcastReceiver", "时间改变接收器收到了null广播");

        else if (action.equals(Intent.ACTION_SCREEN_OFF) || action.equals(Intent.ACTION_TIME_TICK)) {
            Intent stopTimeService = new Intent("stopTimeService");
            context.sendBroadcast(stopTimeService);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Intent service = new Intent(context, TimeService.class);
                    context.startService(service);
                }
            }).start();
        }
    }
}
