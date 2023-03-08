package life.andre.sms487.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;

import life.andre.sms487.R;
import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageContainer;
import life.andre.sms487.network.ServerApi;
import life.andre.sms487.utils.DateUtil;

public class NotificationListener extends NotificationListenerService {
    public static final String TAG = "NTF";
    public static final String CHANNEL_ID = "NotificationListener::ServiceMessage";
    public static final int RUN_ID = 1;

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
        stopForeground(Service.STOP_FOREGROUND_REMOVE);
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

        String appLabel = sbn.getPackageName();
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

    private void createServiceMessage() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "SMS 487 Notification Listener",
                NotificationManager.IMPORTANCE_LOW
        );

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            Logger.w(TAG, "NotificationManager is null");
            return;
        }
        manager.createNotificationChannel(channel);

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("SMS 487")
                .setOngoing(true)
                .setContentText("Service is running for listening notifications")
                .setSmallIcon(R.drawable.ic_notification)
                .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
                .build();

        startForeground(RUN_ID, notification);
    }
}
