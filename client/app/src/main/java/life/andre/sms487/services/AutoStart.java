package life.andre.sms487.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageCleanupWorker;
import life.andre.sms487.messages.MessageResendWorker;

public class AutoStart extends BroadcastReceiver {
    public static final String TAG = AutoStart.class.getSimpleName();

    public void onReceive(@NonNull Context context, @NonNull Intent parentIntent) {
        String action = parentIntent.getAction();
        if (action == null || !action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            return;
        }

        Intent intent = new Intent(context, NotificationListener.class);
        context.startService(intent);

        MessageCleanupWorker.schedule();
        MessageResendWorker.schedule();

        Logger.i(TAG, "Started");
    }
}
