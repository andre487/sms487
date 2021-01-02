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
import life.andre.sms487.messages.MessageStorage;
import life.andre.sms487.network.SmsApi;
import life.andre.sms487.preferences.AppSettings;
import life.andre.sms487.system.AppConstants;
import utils.AsyncTaskUtil;

public class SmsHandler extends Service {
    protected AppSettings appSettings;
    protected MessageStorage messageStorage;
    protected SmsApi smsApi;
    protected SmsRequestListener smsRequestListener;

    private static final String logTag = "SmsHandler";

    @Override
    public void onCreate() {
        appSettings = new AppSettings(this);
        messageStorage = new MessageStorage(this);
        smsApi = new SmsApi(this, appSettings);

        smsRequestListener = new SmsRequestListener(messageStorage);
        smsApi.addRequestHandledListener(smsRequestListener);
    }

    @Override
    public void onDestroy() {
        smsApi.removeRequestHandledListener(smsRequestListener);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!appSettings.getNeedSendSms()) {
            return Service.START_STICKY;
        }

        SendSmsParams params = new SendSmsParams(intent, smsApi, messageStorage, Build.MODEL);
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
        MessageStorage messageStorage;
        String deviceId;

        SendSmsParams(Intent intent, SmsApi smsApi, MessageStorage messageStorage, String deviceId) {
            this.intent = intent;
            this.smsApi = smsApi;
            this.messageStorage = messageStorage;
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
                long insertId = params.messageStorage.addMessage(message);

                params.smsApi.addSms(
                        params.deviceId,
                        message.getDateTime(), message.getSmsCenterDateTime(),
                        message.getAddressFrom(), message.getBody(),
                        insertId
                );
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

    static class MarkSmsSentParams {
        long dbId;
        MessageStorage messageStorage;

        MarkSmsSentParams(long dbId, MessageStorage messageStorage) {
            this.dbId = dbId;
            this.messageStorage = messageStorage;
        }
    }

    static class MarkSmsSentAction extends AsyncTask<MarkSmsSentParams, Void, Void> {
        @Override
        @Nullable
        protected Void doInBackground(MarkSmsSentParams... params) {
            MarkSmsSentParams mainParams = AsyncTaskUtil.getParams(params, logTag);
            if (mainParams == null) {
                return null;
            }

            mainParams.messageStorage.markSent(mainParams.dbId);

            return null;
        }
    }

    static class SmsRequestListener implements SmsApi.RequestHandledListener {
        private final MessageStorage messageStorage;

        public SmsRequestListener(MessageStorage messageStorage) {
            this.messageStorage = messageStorage;
        }

        @Override
        public void onSuccess(long dbId) {
            Logger.i(logTag, "Successfully sent SMS " + dbId);

            MarkSmsSentParams params = new MarkSmsSentParams(dbId, messageStorage);
            new MarkSmsSentAction().execute(params);
        }

        @Override
        public void onError(long dbId, String errorMessage) {
            Logger.e(logTag, "Error with SMS " + dbId + ": " + errorMessage);
        }
    }
}
