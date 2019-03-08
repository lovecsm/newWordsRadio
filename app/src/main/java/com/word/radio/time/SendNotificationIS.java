package com.word.radio.time;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.word.radio.utils.NotificationUtil;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class SendNotificationIS extends IntentService {
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_SEND_MSG = "com.word.radio.time.action.SEND_MSG";

    public static final String MSG_CONTENT = "com.word.radio.time.extra.MSG_CONTENT";
    public static final String MSG_TITLE = "com.word.radio.time.extra.MSG_TITLE";

    public SendNotificationIS() {
        super("SendNotificationIS");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startSendMsg(Context context, String title, String msg) {
        Intent intent = new Intent(context, SendNotificationIS.class);
        intent.setAction(ACTION_SEND_MSG);
        intent.putExtra(MSG_CONTENT, msg);
        intent.putExtra(MSG_TITLE, title);
        context.startService(intent);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SEND_MSG.equals(action)) {
                handleActionSendMsg(intent.getStringExtra(MSG_TITLE),
                        intent.getStringExtra(MSG_CONTENT));
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionSendMsg(String title, String msg) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtil.createNotification(this, title, msg,
                    NotificationUtil.REMIND_LISTEN_CHANNEL_ID);
        } else {
            NotificationUtil.createNotification(this, title, msg);
        }
    }

}
