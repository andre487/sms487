package com.example.andre487.sms487.services;


import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.andre487.sms487.messages.MessageContainer;
import com.example.andre487.sms487.messages.MessageStorage;
import com.example.andre487.sms487.messages.SmsListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class SmsHandler extends Service {
    class HandleMessageParams {
        Intent intent;

        HandleMessageParams(Intent intent) {
            this.intent = intent;
        }
    }

    class HandleMessageAction extends AsyncTask<HandleMessageParams, Void, Void> {
        @Override
        protected Void doInBackground(HandleMessageParams... params) {
            if (params.length == 0) {
                Log.w("SmsHandler", "Params length is 0");
                return null;
            }
            HandleMessageParams mainParams = params[0];

            ArrayList<String> intentData = mainParams.intent.getStringArrayListExtra(
                    SmsListener.EXTRA_GOT_SMS
            );

            if (intentData == null) {
                Log.w("SmsHandler", "Intent data is null");
                return null;
            }

            handleIntentData(intentData);

            return null;
        }

        private void handleIntentData(ArrayList<String> intentData) {
            ArrayList<MessageContainer> messages = extractMessages(intentData);

            messageStorage.addMessages(messages);
        }

        private ArrayList<MessageContainer> extractMessages(ArrayList<String> intentData) {
            ArrayList<MessageContainer> data = new ArrayList<>();

            for (String messageJson : intentData) {
                Log.d("SmsHandler", "Got message: " + messageJson);

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

    MessageStorage messageStorage;

    SmsHandler() {
        super();

        messageStorage = new MessageStorage(this);
    }

    @Override
    public void onCreate() {
        Log.d("SmsHandler", "Service started");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        HandleMessageParams params = new HandleMessageParams(intent);
        new HandleMessageAction().execute(params);

        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
