package life.andre.sms487.services.notificationListener;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import androidx.core.app.NotificationCompat;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageStorage;
import life.andre.sms487.network.SmsApi;
import life.andre.sms487.preferences.AppSettings;
import life.andre.sms487.services.smsHandler.SmsRequestListener;

public class NotificationListener extends NotificationListenerService {
    protected AppSettings appSettings;
    protected MessageStorage messageStorage;
    protected SmsApi smsApi;
    protected SmsRequestListener smsRequestListener;

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.d("NotificationListener", "Service is started");

        appSettings = new AppSettings(this);
        messageStorage = new MessageStorage(this);
        smsApi = new SmsApi(this, appSettings);

        smsRequestListener = new SmsRequestListener(messageStorage, "NotificationListener");
        smsApi.addRequestHandledListener(smsRequestListener);

        createServiceMessage();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        smsApi.removeRequestHandledListener(smsRequestListener);

        stopForeground(true);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        if (!isNotificationSuitable(sbn)) {
            return;
        }

        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);

        if (title == null && text == null) {
            Logger.e("NotificationListener", "No text in message");
            return;
        }

        String titleText = title == null ? "" : title.toString();
        String textText = text == null ? "" : text.toString();

        String fullText = (titleText + "\n" + textText).trim();
        if (fullText.isEmpty()) {
            Logger.e("NotificationListener", "No text in message");
            return;
        }

        String appLabel = getAppLabel(sbn.getPackageName());
        long postTime = sbn.getPostTime();
        String deviceId = Build.MODEL;

        SendNotificationParams params = new SendNotificationParams(
                smsApi, messageStorage, appLabel, postTime, fullText, deviceId
        );
        new SendNotificationAction().execute(params);

        Logger.d("NotificationListener", "Notification: " + text);
    }

    boolean isNotificationSuitable(StatusBarNotification sbn) {
        if (!sbn.isClearable()) {
            return false;
        }

        String tag = sbn.getTag();
        return tag == null || !tag.contains(":sms:");
    }

    public String getAppLabel(String packageName) {
        ApplicationInfo applicationInfo = null;

        PackageManager packageManager = this.getPackageManager();
        try {
            applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            Logger.e("NotificationListener", "Get app name error: " + e.getMessage());
        }

        if (applicationInfo == null) {
            return packageName;
        }

        return (String) packageManager.getApplicationLabel(applicationInfo);
    }

    private void createServiceMessage() {
        String channelId = "NotificationListener::ServiceMessage";
        NotificationChannel channel = new NotificationChannel(
                channelId, "SMS487 Notification Listener",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nManager == null) {
            Logger.e("NotificationListener", "NotificationManager is null");
            return;
        }
        nManager.createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("")
                .setContentText("")
                .build();

        startForeground(1, notification);
    }
}
