package life.andre.sms487.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import life.andre.sms487.logging.Logger;

public class AutoStart extends BroadcastReceiver {
    public static final String TAG = "ASR";

    public void onReceive(@NonNull Context context, @NonNull Intent parentIntent) {
        String action = parentIntent.getAction();
        if (action == null || !action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            return;
        }

        Logger.i(TAG, "AutoStart received");
        // In ApplicationEntryPoint.startServiceTasks important tasks will be launched
    }
}
