package life.andre.sms487.services.smsHandler;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageStorage;
import life.andre.sms487.network.SmsApi;
import life.andre.sms487.preferences.AppSettings;

public class SmsHandler extends Service {
    protected AppSettings appSettings;
    protected MessageStorage messageStorage;
    protected SmsApi smsApi;
    protected SmsRequestListener smsRequestListener;

    @Override
    public void onCreate() {
        Logger.d("SmsHandler", "Service started");

        appSettings = new AppSettings(this);
        messageStorage = new MessageStorage(this);
        smsApi = new SmsApi(this, appSettings);

        smsRequestListener = new SmsRequestListener(messageStorage, "SmsHandler");
        smsApi.addRequestHandledListener(smsRequestListener);
    }

    @Override
    public void onDestroy() {
        smsApi.removeRequestHandledListener(smsRequestListener);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        SendSmsParams params = new SendSmsParams(intent, smsApi, messageStorage, Build.MODEL);
        new SendSmsAction().execute(params);

        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
