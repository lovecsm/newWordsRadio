package com.word.radio.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.word.radio.newradio.MainActivity;
import com.word.radio.newradio.R;

import static android.content.Context.NOTIFICATION_SERVICE;

public class NotificationUtil {
    private static Notification notification;
    private static NotificationManager notificationManager;
    private static NotificationManager manager;

    /**
     * 针对API版本较低的创建通知方法
     *
     * @param context     上下文
     * @param title       通知标题
     * @param msg         通知消息
     * @param remoteViews 通知栏的remoteView
     */
    public static void getNotification(Context context, String title, String msg, RemoteViews remoteViews) {
        // 对API等级限制，低于安卓O的无法使用该方法
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return;
        }
        notification = new NotificationCompat.Builder(context)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker("通知来了")
                .setContentTitle(title)
                .setContentText(msg)
                .setWhen(System.currentTimeMillis())
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(PendingIntent.getActivity(context, 1, new Intent(context, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT))
                .setCustomContentView(remoteViews)
                .build();
        notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }

    /**
     * 针对API大于安卓O的创建通知方法
     * @param context 上下文
     * @param title 通知标题
     * @param msg 通知消息
     * @param remoteViews 通知栏的remoteView
     * @param channelID 通知渠道ID
     * @param channelName 渠道名
     */
    public static void getNotification(Context context, String title, String msg, RemoteViews remoteViews, String channelID, String channelName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelID, channelName, NotificationManager.IMPORTANCE_HIGH);
            manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
            notification = new Notification.Builder(context, channelID)
                    .setWhen(System.currentTimeMillis())
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentText(msg)
                    .setContentTitle(title)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setCustomContentView(remoteViews)
                    .setChannelId(channelID)
                    .setContentIntent(PendingIntent.getActivity(context, 1, new Intent(context, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT))
                    .build();
            manager.notify(0, notification);
        }
    }

    /**
     * API>=安卓O的取消通知方法
     */
    public static void cancelNotificationHigh() {
        if (manager == null) {
            return;
        }
        manager.cancel(0);
        destroy();
    }

    /**
     * API等级低时的取消通知方法
     */
    public static void cancelNotification() {
        if (notificationManager == null) {
            return;
        }
        notificationManager.cancel(0);
        destroy();
    }

    private static void destroy() {
        notification = null;
        notificationManager = null;
        manager = null;
    }
}
