package life.andre.sms487.services.notificationListener;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import life.andre.sms487.logging.Logger;

public class NotificationListener extends NotificationListenerService {
    @Override
    public void onCreate() {
        super.onCreate();
        Logger.d("NotificationListener", "Service is started");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        Logger.d("NotificationListener", "Notification!!!");
    }
}
