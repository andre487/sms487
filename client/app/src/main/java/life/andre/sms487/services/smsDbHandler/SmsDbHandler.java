package life.andre.sms487.services.smsDbHandler;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageStorage;

public class SmsDbHandler extends Service {
    protected MessageStorage messageStorage = new MessageStorage(this);

    @Override
    public void onCreate() {
        Logger.d("SmsDbHandler", "Service started");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        HandleMessageParams params = new HandleMessageParams(intent, messageStorage);
        new HandleMessageAction().execute(params);

        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
