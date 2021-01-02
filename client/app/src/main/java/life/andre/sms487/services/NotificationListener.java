package life.andre.sms487.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageContainer;
import life.andre.sms487.network.SmsApi;
import life.andre.sms487.system.AppSettings;
import life.andre.sms487.utils.AsyncTaskUtil;

public class NotificationListener extends NotificationListenerService {
    protected AppSettings appSettings;
    protected SmsApi smsApi;

    private static final String logTag = "NotificationListener";

    @Override
    public void onCreate() {
        super.onCreate();
        appSettings = new AppSettings(this);
        smsApi = new SmsApi(this, appSettings);

        createServiceMessage();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        if (!isNotificationSuitable(sbn)) {
            return;
        }

        Notification notification = sbn.getNotification();
        if (sbn == null) {
            Logger.w(logTag, "Sbn is null");
            return;
        }

        Bundle extras = notification.extras;
        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);

        if (title == null && text == null) {
            Logger.w(logTag, "No text in message");
            return;
        }

        String titleText = title == null ? "" : title.toString();
        String textText = text == null ? "" : text.toString();

        String fullText = (titleText + "\n" + textText).trim();
        if (fullText.isEmpty()) {
            Logger.w(logTag, "No text in message");
            return;
        }

        String appLabel = getAppLabel(sbn.getPackageName());
        long postTime = sbn.getPostTime();
        String deviceId = Build.MODEL;

        SendNotificationParams params = new SendNotificationParams(
                smsApi, appLabel, postTime, fullText, deviceId
        );
        new SendNotificationAction().execute(params);
    }

    boolean isNotificationSuitable(StatusBarNotification sbn) {
        return sbn.isClearable();
    }

    public String getAppLabel(String packageName) {
        ApplicationInfo applicationInfo = null;

        PackageManager packageManager = this.getPackageManager();
        try {
            applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            Logger.e(logTag, "Get app name error: " + e.getMessage());
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

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            Logger.w(logTag, "NotificationManager is null");
            return;
        }
        manager.createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("")
                .setContentText("")
                .build();

        startForeground(1, notification);
    }

    static class SendNotificationParams {
        SmsApi smsApi;

        String appLabel;
        long postTime;
        String text;
        String deviceId;

        SendNotificationParams(SmsApi smsApi, String appLabel, long postTime, String text, String deviceId) {
            this.smsApi = smsApi;
            this.appLabel = appLabel;
            this.postTime = postTime;
            this.text = text;
            this.deviceId = deviceId;
        }
    }

    static class SendNotificationAction extends AsyncTask<SendNotificationParams, Void, Void> {
        private SimpleDateFormat dateFormat;

        @SuppressLint("SimpleDateFormat")
        @Override
        protected Void doInBackground(SendNotificationParams... params) {
            dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm Z");  // TODO: extract to life.andre.sms487.utils
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            SendNotificationParams mainParams = AsyncTaskUtil.getParams(params, logTag);
            if (mainParams == null) {
                return null;
            }

            handleNotification(mainParams);

            return null;
        }

        void handleNotification(SendNotificationParams params) {
            String curTime = dateFormat.format(new Date());
            String postTime = dateFormat.format(new Date(params.postTime));

            MessageContainer message = new MessageContainer(
                    params.deviceId, params.appLabel, curTime, postTime,
                    params.text, false, 0
            );
            params.smsApi.addNotification(message);
        }
    }
}
