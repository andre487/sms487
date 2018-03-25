package com.example.andre487.sms487.messages;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;

import com.example.andre487.sms487.services.SmsAdder;
import com.example.andre487.sms487.services.SmsHandler;

import java.util.ArrayList;

public class SmsListener extends BroadcastReceiver {
    public static final String EXTRA_GOT_SMS = "com.example.andre487.EXTRA_GOT_SMS";

    class HandleMessageParams {
        Context context;
        Intent intent;

        HandleMessageParams(Context context, Intent intent) {
            this.context = context;
            this.intent = intent;
        }
    }

    class HandleMessageAction extends AsyncTask<HandleMessageParams, Void, Void> {
        @Override
        protected Void doInBackground(HandleMessageParams... params) {
            if (params.length == 0) {
                Log.w("SmsListener", "Params length is 0");
                return null;
            }
            HandleMessageParams mainParams = params[0];

            MessageContainer[] messages = extractMessages(mainParams.intent);
            if (messages != null) {
                sendMessagesToHandler(mainParams.context, messages);
            }

            return null;
        }

        private MessageContainer[] extractMessages(Intent intent) {
            String action = intent.getAction();
            if (action == null || !action.equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
                return null;
            }

            Bundle bundle = intent.getExtras();
            if (bundle == null) {
                Log.w("SmsListener", "Bundle is null");
                return null;
            }

            Object[] pdus = (Object[]) bundle.get("pdus");
            String format = bundle.getString("format");
            if (pdus == null || format == null) {
                Log.w("SmsListener", "PDUs or format is null");
                return null;
            }

            MessageContainer[] messages = new MessageContainer[pdus.length];
            for (int i = 0; i < messages.length; ++i) {
                MessageContainer message = messages[i] = new MessageContainer(pdus[i], format);

                Log.d("SmsListener", "Message address: " + message.getAddressFrom());
                Log.d("SmsListener", "Message body: " + message.getBody());
            }

            Log.i("SmsListener", "Receive messages");

            return messages;
        }

        private void sendMessagesToHandler(Context context, MessageContainer[] messages) {
            Intent baseIntent = new Intent();

            ArrayList<String> intentData = new ArrayList<>();
            for (MessageContainer message : messages) {
                String messageJson = message.toString();
                if (messageJson != null) {
                    intentData.add(messageJson);
                }
            }

            baseIntent.putStringArrayListExtra(EXTRA_GOT_SMS, intentData);

            Intent smsHandlerIntent = new Intent(baseIntent);
            smsHandlerIntent.setClass(context, SmsHandler.class);
            context.startService(smsHandlerIntent);

            Intent smsAdderIntent = new Intent(baseIntent);
            smsAdderIntent.setClass(context, SmsAdder.class);
            context.startService(smsAdderIntent);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        HandleMessageParams params = new HandleMessageParams(context, intent);

        new HandleMessageAction().execute(params);
    }
}
