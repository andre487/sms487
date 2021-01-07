package life.andre.sms487.services;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageContainer;
import life.andre.sms487.network.ServerApi;
import life.andre.sms487.system.AppConstants;
import life.andre.sms487.system.AppSettings;
import life.andre.sms487.utils.AsyncTaskUtil;

public class SmsHandler extends Service {
    public static final String TAG = SmsHandler.class.getSimpleName();

    protected AppSettings appSettings;
    protected ServerApi serverApi;

    @Override
    public void onCreate() {
        appSettings = new AppSettings(this);
        serverApi = new ServerApi(this, appSettings);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!appSettings.getNeedSendSms()) {
            return Service.START_STICKY;
        }

        SendSmsParams params = new SendSmsParams(intent, serverApi);
        new SendSmsAction().execute(params);

        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static class SendSmsParams {
        final Intent intent;
        final ServerApi serverApi;

        SendSmsParams(Intent intent, ServerApi serverApi) {
            this.intent = intent;
            this.serverApi = serverApi;
        }
    }

    static class SendSmsAction extends AsyncTask<SendSmsParams, Void, Void> {
        @Nullable
        @Override
        protected Void doInBackground(@NonNull SendSmsParams... params) {
            SendSmsParams mainParams = AsyncTaskUtil.getParams(params, TAG);
            if (mainParams == null) {
                return null;
            }

            Intent intent = mainParams.intent;
            if (intent == null) {
                Logger.w(TAG, "Intent is null");
                return null;
            }

            List<String> intentData = intent.getStringArrayListExtra(AppConstants.EXTRA_GOT_SMS);
            if (intentData == null) {
                Logger.w(TAG, "Intent data is null");
                return null;
            }

            handleIntentData(mainParams, intentData);

            return null;
        }

        private void handleIntentData(@NonNull SendSmsParams params, @NonNull List<String> intentData) {
            List<MessageContainer> extractedMessages = extractMessages(intentData);

            for (MessageContainer message : extractedMessages) {
                params.serverApi.addMessage(message);
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
}
