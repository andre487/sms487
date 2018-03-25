package com.example.andre487.sms487.services;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.andre487.sms487.messages.MessageContainer;
import com.example.andre487.sms487.messages.SmsListener;
import com.example.andre487.sms487.network.SmsApi;
import com.example.andre487.sms487.preferences.AppSettings;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class SmsAdder extends Service {
    static class AddSmsParams {
        Intent intent;
        SmsApi smsApi;

        AddSmsParams(Intent intent, SmsApi smsApi) {
            this.intent = intent;
            this.smsApi = smsApi;
        }
    }

    static class AddSmsAction extends AsyncTask<AddSmsParams, Void, Void> {
        @Override
        protected Void doInBackground(AddSmsParams... params) {
            if (params.length == 0) {
                Log.w("SmsAdder", "Params length is 0");
                return null;
            }
            AddSmsParams mainParams = params[0];

            ArrayList<String> intentData = mainParams.intent.getStringArrayListExtra(
                    SmsListener.EXTRA_GOT_SMS
            );

            if (intentData == null) {
                Log.w("SmsAdder", "Intent data is null");
                return null;
            }

            handleIntentData(mainParams.smsApi, intentData);

            return null;
        }

        private void handleIntentData(SmsApi smsApi, ArrayList<String> intentData) {
            for (MessageContainer message : extractMessages(intentData)) {
                smsApi.addSms(
                        "test2",
                        message.getDateTime(),
                        message.getAddressFrom(),
                        message.getBody()
                );
            }
        }

        private ArrayList<MessageContainer> extractMessages(ArrayList<String> intentData) {
            ArrayList<MessageContainer> data = new ArrayList<>();

            for (String messageJson : intentData) {
                Log.d("SmsAdder", "Got message: " + messageJson);

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

    SmsApi smsApi;

    @Override
    public void onCreate() {
        Log.d("SmsAdder", "Service started");

        AppSettings appSettings = new AppSettings(this);
        smsApi = new SmsApi(
                this,
                appSettings.getServerUrl(),
                "sms487",
                appSettings.getServerKey()
        );
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        AddSmsParams params = new AddSmsParams(intent, smsApi);
        new AddSmsAction().execute(params);

        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
