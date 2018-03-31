package life.andre.sms487.services.smsApiSender;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.network.SmsApi;
import life.andre.sms487.preferences.AppSettings;

public class SmsApiSender extends Service {
    private AppSettings appSettings;

    @Override
    public void onCreate() {
        Logger.d("SmsApiSender", "Service started");

        appSettings = new AppSettings(this);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        SmsApi smsApi = new SmsApi(
                this,
                appSettings.getServerUrl(),
                appSettings.getUserName(),
                appSettings.getServerKey()
        );

        SendSmsParams params = new SendSmsParams(intent, smsApi, Build.MODEL);
        new SendSmsAction().execute(params);

        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
