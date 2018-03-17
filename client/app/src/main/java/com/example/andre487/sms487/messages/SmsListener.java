package com.example.andre487.sms487.messages;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;

public class SmsListener extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null || !action.equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            Log.w("SmsListener", "Bundle is null");
            return;
        }

        Object[] pdus = (Object[]) bundle.get("pdus");
        String format = bundle.getString("format");
        if (pdus == null || format == null) {
            Log.w("SmsListener", "PDUs or format is null");
            return;
        }

        MessageContainer[] messages = new MessageContainer[pdus.length];
        for (int i = 0; i < messages.length; ++i) {
            MessageContainer message = messages[i] = new MessageContainer(pdus[i], format);

            Log.d("SmsListener", "Message address: " + message.getAddressFrom());
            Log.d("SmsListener", "Message body: " + message.getBody());
        }

        Log.i("SmsListener", "Receive messages");
    }
}
