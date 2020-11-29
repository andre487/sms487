package life.andre.sms487.services.autoStart;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.services.notificationListener.NotificationListener;

public class AutoStart extends BroadcastReceiver {
    public void onReceive(Context context, Intent parentIntent) {
        String action = parentIntent.getAction();
        if (action == null || !action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            return;
        }

        Intent intent = new Intent(context, NotificationListener.class);
        context.startService(intent);

        Logger.d("AutoStart", "Started");
    }
}
