package life.andre.sms487.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageContainer;
import life.andre.sms487.network.ServerApi;
import life.andre.sms487.settings.AppSettings;
import life.andre.sms487.system.AppConstants;
import life.andre.sms487.utils.BgTask;

public class SmsHandler extends Service {
    public static final String TAG = "SmsHandler";

    protected ServerApi serverApi;

    @Override
    public void onCreate() {
        serverApi = ServerApi.getInstance();
    }

    public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
        BgTask.run(() -> {
            handleIntent(intent);
            return null;
        });
        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleIntent(@NonNull Intent intent) {
        boolean needSend = AppSettings.getInstance().getNeedSendSms();
        if (!needSend) {
            return;
        }

        List<String> intentData = intent.getStringArrayListExtra(AppConstants.EXTRA_GOT_SMS);
        if (intentData == null) {
            Logger.w(TAG, "Intent data is null");
            return;
        }

        for (MessageContainer message : extractMessages(intentData)) {
            serverApi.addMessage(message);
        }
    }

    @NonNull
    private List<MessageContainer> extractMessages(@NonNull List<String> intentData) {
        List<MessageContainer> data = new ArrayList<>();

        for (String messageJson : intentData) {
            MessageContainer message = MessageContainer.createFromJson(messageJson);
            if (message != null) {
                data.add(message);
            }
        }

        return data;
    }
}
