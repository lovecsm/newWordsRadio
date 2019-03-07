package com.word.radio.time;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class FinishVoiceIS extends IntentService {
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_FINISH = "com.word.radio.time.action.FINISH";


    public FinishVoiceIS() {
        super("FinishVoiceIS");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startFinish(Context context) {
        Intent intent = new Intent(context, FinishVoiceIS.class);
        intent.setAction(ACTION_FINISH);
        context.startService(intent);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_FINISH.equals(action)) {
                handleActionFinish();
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFinish() {
        //System.exit(0);
        Intent finishIntent = new Intent("ok");
        sendBroadcast(finishIntent);
    }

}
