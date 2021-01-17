package life.andre.sms487.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;

import life.andre.sms487.R;
import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageContainer;
import life.andre.sms487.network.ServerApi;
import life.andre.sms487.system.AppConstants;
import life.andre.sms487.utils.DateUtil;

public class NotificationListener extends NotificationListenerService {
    public static final String TAG = "NTF";
    public static final String CHANNEL_ID = "NotificationListener::ServiceMessage";

    protected ServerApi serverApi;

    @Override
    public void onCreate() {
        super.onCreate();
        serverApi = ServerApi.getInstance();

        createServiceMessage();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    @Override
    public void onNotificationPosted(@NonNull StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        if (!isNotificationSuitable(sbn)) {
            return;
        }

        Notification notification = sbn.getNotification();

        Bundle extras = notification.extras;
        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);

        if (title == null && text == null) {
            Logger.w(TAG, "No text in message");
            return;
        }

        String titleText = title == null ? "" : title.toString();
        String textText = text == null ? "" : text.toString();

        String fullText = (titleText + "\n" + textText).trim();
        if (fullText.isEmpty()) {
            Logger.w(TAG, "No text in message");
            return;
        }

        String appLabel = getAppLabel(sbn.getPackageName());
        long postTime = sbn.getPostTime();

        String curTime = DateUtil.nowFormatted();
        String postTimeString = DateUtil.formatDate(postTime);

        serverApi.addMessage(new MessageContainer(
                ServerApi.MESSAGE_TYPE_NOTIFICATION,
                appLabel, curTime, postTimeString, fullText
        ));
    }

    boolean isNotificationSuitable(@NonNull StatusBarNotification sbn) {
        return sbn.isClearable();
    }

    @NonNull
    public String getAppLabel(@NonNull String packageName) {
        ApplicationInfo applicationInfo = null;

        PackageManager packageManager = this.getPackageManager();
        try {
            applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        } catch (@NonNull final PackageManager.NameNotFoundException e) {
            Logger.e(TAG, "Get app name error: " + e.getMessage());
        }

        if (applicationInfo == null) {
            return packageName;
        }

        return (String) packageManager.getApplicationLabel(applicationInfo);
    }

    private void createServiceMessage() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "SMS 487 Notification Listener",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            Logger.w(TAG, "NotificationManager is null");
            return;
        }
        manager.createNotificationChannel(channel);

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("SMS 487")
                .setContentText("Service is running for listening notifications")
                .setSmallIcon(R.drawable.ic_notification)
                .build();

        startForeground(AppConstants.DEFAULT_ID, notification);
    }
}
