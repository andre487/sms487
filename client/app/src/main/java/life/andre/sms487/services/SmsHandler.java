package life.andre.sms487.services;


import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;

import life.andre.sms487.IntentTypes;
import life.andre.sms487.logging.Logger;
import life.andre.sms487.messageStorage.MessageContainer;
import life.andre.sms487.messageStorage.MessageStorage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class SmsHandler extends Service {
    static class HandleMessageParams {
        Intent intent;
        MessageStorage messageStorage;
        HashMap<Activity, Handler> activityHandlersMap;

        HandleMessageParams(Intent intent, MessageStorage messageStorage,
                            HashMap<Activity, Handler> activityHandlersMap) {
            this.intent = intent;
            this.messageStorage = messageStorage;
            this.activityHandlersMap = activityHandlersMap;
        }
    }

    static class HandleMessageAction extends AsyncTask<HandleMessageParams, Void, Void> {
        @Override
        protected Void doInBackground(HandleMessageParams... params) {
            if (params.length == 0) {
                Logger.w("SmsHandler", "Params length is 0");
                return null;
            }

            HandleMessageParams mainParams = params[0];
            ArrayList<String> intentData = mainParams.intent.getStringArrayListExtra(
                    IntentTypes.EXTRA_GOT_SMS
            );

            if (intentData == null) {
                Logger.w("SmsHandler", "Intent data is null");
                return null;
            }

            handleIntentData(intentData, mainParams.messageStorage);
            propagateNewSms(mainParams.activityHandlersMap);

            return null;
        }


        private void handleIntentData(ArrayList<String> intentData, MessageStorage messageStorage) {
            ArrayList<MessageContainer> messages = extractMessages(intentData);

            messageStorage.addMessages(messages);
        }

        private ArrayList<MessageContainer> extractMessages(ArrayList<String> intentData) {
            ArrayList<MessageContainer> data = new ArrayList<>();

            for (String messageJson : intentData) {
                Logger.d("SmsHandler", "Got message: " + messageJson);

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

        private void propagateNewSms(HashMap<Activity, Handler> activityHandlersMap) {
            for (Handler handler : activityHandlersMap.values()) {
                handler.dispatchMessage(new Message());
            }
        }
    }

    public class SmsBridge extends Binder {
        public SmsHandler getService() {
            return SmsHandler.this;
        }
    }

    protected IBinder smsBridge = new SmsBridge();
    protected MessageStorage messageStorage = new MessageStorage(this);
    protected HashMap<Activity, Handler> activityHandlersMap = new HashMap<>();

    @Override
    public void onCreate() {
        Logger.d("SmsHandler", "Service started");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        HandleMessageParams params = new HandleMessageParams(
                intent,
                messageStorage,
                activityHandlersMap
        );
        new HandleMessageAction().execute(params);

        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return smsBridge;
    }

    public void setNewSmsHandler(Activity activity, Handler handler) {
        this.activityHandlersMap.put(activity, handler);
    }

    public void removeNewSmsHandler(Activity activity) {
        this.activityHandlersMap.remove(activity);
    }
}
