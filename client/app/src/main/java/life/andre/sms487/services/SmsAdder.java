package life.andre.sms487.services;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;

import life.andre.sms487.IntentTypes;
import life.andre.sms487.logging.Logger;
import life.andre.sms487.messageStorage.MessageContainer;
import life.andre.sms487.network.SmsApi;
import life.andre.sms487.preferences.AppSettings;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class SmsAdder extends Service {
    static class AddSmsParams {
        Intent intent;
        SmsApi smsApi;
        String deviceId;

        AddSmsParams(Intent intent, SmsApi smsApi, String deviceId) {
            this.intent = intent;
            this.smsApi = smsApi;
            this.deviceId = deviceId;
        }
    }

    static class AddSmsAction extends AsyncTask<AddSmsParams, Void, Void> {
        @Override
        protected Void doInBackground(AddSmsParams... params) {
            if (params.length == 0) {
                Logger.w("SmsAdder", "Params length is 0");
                return null;
            }
            AddSmsParams mainParams = params[0];

            ArrayList<String> intentData = mainParams.intent.getStringArrayListExtra(
                    IntentTypes.EXTRA_GOT_SMS
            );

            if (intentData == null) {
                Logger.w("SmsAdder", "Intent data is null");
                return null;
            }

            handleIntentData(mainParams.smsApi, mainParams.deviceId, intentData);

            return null;
        }

        private void handleIntentData(SmsApi smsApi, String deviceId, ArrayList<String> intentData) {
            for (MessageContainer message : extractMessages(intentData)) {
                smsApi.addSms(
                        deviceId,
                        message.getDateTime(),
                        message.getAddressFrom(),
                        message.getBody()
                );
            }
        }

        private ArrayList<MessageContainer> extractMessages(ArrayList<String> intentData) {
            ArrayList<MessageContainer> data = new ArrayList<>();

            for (String messageJson : intentData) {
                Logger.d("SmsAdder", "Got message: " + messageJson);

                try {
                    JSONObject obj = new JSONObject(messageJson);

                    String addressFrom = (String) obj.get("address_from");
                    String dateTime = (String) obj.get("date_time");
                    String body = (String) obj.get("body");

                    MessageContainer message = new MessageContainer(addressFrom, dateTime, body);
                    data.add(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            return data;
        }
    }

    private AppSettings appSettings;

    @Override
    public void onCreate() {
        Logger.d("SmsAdder", "Service started");

        appSettings = new AppSettings(this);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        SmsApi smsApi = new SmsApi(
                this,
                appSettings.getServerUrl(),
                appSettings.getUserName(),
                appSettings.getServerKey()
        );

        AddSmsParams params = new AddSmsParams(intent, smsApi, Build.MODEL);
        new AddSmsAction().execute(params);

        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
