package com.word.radio.time;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.word.radio.utils.LogUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class TimeChangeReceiver extends BroadcastReceiver {

    private static final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null)
            LogUtils.i("TimeBroadcastReceiver", "时间改变接收器收到了null广播");

        else if (action.equals(Intent.ACTION_SCREEN_OFF) || action.equals(Intent.ACTION_TIME_TICK)) {
            Intent stopTimeService = new Intent("stopTimeService");
            context.sendBroadcast(stopTimeService);
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    SystemClock.sleep(1000);
                    Intent service = new Intent(context, TimeService.class);
                    context.startService(service);
                }
            };
            singleThreadExecutor.execute(runnable);
        }
    }
}
