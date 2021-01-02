package life.andre.sms487.services;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageContainer;
import life.andre.sms487.network.SmsApi;
import life.andre.sms487.system.AppSettings;
import life.andre.sms487.system.AppConstants;
import life.andre.sms487.utils.AsyncTaskUtil;

public class SmsHandler extends Service {
    protected AppSettings appSettings;
    protected SmsApi smsApi;
    private static final String logTag = "SmsHandler";

    @Override
    public void onCreate() {
        appSettings = new AppSettings(this);
        smsApi = new SmsApi(this, appSettings);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!appSettings.getNeedSendSms()) {
            return Service.START_STICKY;
        }

        SendSmsParams params = new SendSmsParams(intent, smsApi, Build.MODEL);
        new SendSmsAction().execute(params);

        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static class SendSmsParams {
        Intent intent;
        SmsApi smsApi;
        String deviceId;

        SendSmsParams(Intent intent, SmsApi smsApi, String deviceId) {
            this.intent = intent;
            this.smsApi = smsApi;
            this.deviceId = deviceId;
        }
    }

    static class SendSmsAction extends AsyncTask<SendSmsParams, Void, Void> {
        @Override
        protected Void doInBackground(SendSmsParams... params) {
            SendSmsParams mainParams = AsyncTaskUtil.getParams(params, logTag);
            if (mainParams == null) {
                return null;
            }

            Intent intent = mainParams.intent;
            if (intent == null) {
                Logger.w(logTag, "Intent is null");
                return null;
            }

            List<String> intentData = intent.getStringArrayListExtra(AppConstants.EXTRA_GOT_SMS);
            if (intentData == null) {
                Logger.w(logTag, "Intent data is null");
                return null;
            }

            handleIntentData(mainParams, intentData);

            return null;
        }

        private void handleIntentData(SendSmsParams params, List<String> intentData) {
            List<MessageContainer> extractedMessages = extractMessages(intentData);

            for (MessageContainer message : extractedMessages) {
                params.smsApi.addSms(message);
            }
        }

        private List<MessageContainer> extractMessages(List<String> intentData) {
            List<MessageContainer> data = new ArrayList<>();

            for (String messageJson : intentData) {
                Logger.d(logTag, "Got message: " + messageJson);

                MessageContainer message = MessageContainer.createFromJson(messageJson);
                if (message != null) {
                    data.add(message);
                }
            }

            return data;
        }
    }
}
