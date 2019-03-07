package com.word.radio.time;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import com.word.radio.newradio.R;

import java.text.SimpleDateFormat;
import java.util.Locale;

import static com.word.radio.time.SendNotificationIS.ACTION_SEND_MSG;
import static com.word.radio.time.SendNotificationIS.MSG_CONTENT;
import static com.word.radio.time.SendNotificationIS.MSG_TITLE;

public class TimeService extends Service {
    private TimeChangeReceiver receiver;
    private TimeServiceBroadcastReceiver tsbr;
    private AlarmManager manager;
    private PendingIntent pi;
    //private WakeLock wakeLock;

    public TimeService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("TimeService", "时间后台服务被创建。注册广播");
        tsbr = new TimeServiceBroadcastReceiver();
        registerReceiver(tsbr, new IntentFilter("stopTimeService"));
        //服务启动广播接收器,使得广播接收器可以在程序退出后在后台继续执行,接收系统时间或锁屏事件变更广播事件
        receiver = new TimeChangeReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receiver, intentFilter);

        manager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // 创建PendingIntent对象和Intent,用来自动启动发送通知的IntentService
        Intent sendNotIntent = new Intent(getApplicationContext(), SendNotificationIS.class);
        sendNotIntent.setAction(ACTION_SEND_MSG);
        // 设置通知内容
        sendNotIntent.putExtra(MSG_CONTENT, getString(R.string.remind_listen_word_msg));
        // 设置通知标题
        sendNotIntent.putExtra(MSG_TITLE, getString(R.string.remind_listen_word_title));
        pi = PendingIntent.getService(TimeService.this, 0, sendNotIntent, 0);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences sp = getApplication().getSharedPreferences("remind_time", Context.MODE_PRIVATE);
        String customTime = sp.getString("time", "00:00");
        long sleepTime;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm", Locale.CHINA);
        long currentTimeMillis = System.currentTimeMillis();
        String currentTime = simpleDateFormat.format(currentTimeMillis);
        Log.i("LaunchService", "当前时间：" + currentTime);
        Log.i("LaunchService", "用户定义时间：" + customTime);
        sleepTime = getSleepTime(currentTime, customTime != null ? customTime : "00:00");
        Log.i("TimeService", "时间服务后台进程已经设置好闹钟，" + sleepTime / 1000 / 3600 + "时" +
                sleepTime / 1000 % 3600 / 60 + "分" + sleepTime / 1000 % 3600 % 60 + "秒之后启动SendNotificationIS");

        sleepTime += currentTimeMillis;

        // 设置一个闹钟，直到sleepTime之时唤醒手机并执行pi启动SendNotificationIS发送通知
        manager.setExact(AlarmManager.RTC_WAKEUP, sleepTime, pi);

        return super.onStartCommand(intent, flags, startId);
    }

    public static long getSleepTime(String from, String to) {
        int nowHour = Integer.parseInt(from.split(":")[0]);
        int nowMinute = Integer.parseInt(from.split(":")[1]);
        int futureHour = Integer.parseInt(to.split(":")[0]);
        int futureMinute = Integer.parseInt(to.split(":")[1]);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        long ext = Integer.parseInt(simpleDateFormat.format(System.currentTimeMillis()).split(":")[2]) * 1000;
        if (nowHour > futureHour) {
            // 24:00 - nowTime + futureTime
            return (3600 * 24 - (nowHour * 3600 + nowMinute * 60) + (futureHour * 3600 + futureMinute * 60)) * 1000 - ext;
        } else if (nowHour < futureHour) {
            // futureTime - nowTime
            return (futureHour * 3600 + futureMinute * 60 - (nowHour * 3600 + nowMinute * 60)) * 1000 - ext;
        } else if (futureMinute > nowMinute) {
            // futureMinute - nowMinute
            return (futureMinute - nowMinute) * 60000 - ext;
        } else if (futureMinute < nowMinute) {
            return ((24 * 60 - (nowMinute - futureMinute)) * 60000 - ext);
        } else {
            return 0;
        }
    }

    @Override
    public void onDestroy() {
        Log.i("TimeService", "TimeService后台进程和自身广播被销毁了。TimeChangeReceiver被销毁了。闹钟被销毁了。");
        super.onDestroy();
        unregisterReceiver(receiver);
        unregisterReceiver(tsbr);
        manager.cancel(pi);
        //wakeLock.release();
    }

    public class TimeServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            TimeService.this.stopSelf();
            Log.i("TimeService", "收到关闭TimeService的广播");
        }
    }
}
