package life.andre.sms487.smsListener;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Telephony;

import life.andre.sms487.AppConstants;
import life.andre.sms487.logging.Logger;
import life.andre.sms487.messageStorage.MessageContainer;
import life.andre.sms487.services.smsApiSender.SmsApiSender;
import life.andre.sms487.services.smsDbHandler.SmsDbHandler;

class HandleMessageAction extends AsyncTask<HandleMessageParams, Void, Void> {
    @Override
    protected Void doInBackground(HandleMessageParams... params) {
        if (params.length == 0) {
            Logger.w("SmsListener", "Params length is 0");
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
            Logger.w("SmsListener", "Bundle is null");
            return null;
        }

        Object[] pdus = (Object[]) bundle.get("pdus");
        String format = bundle.getString("format");
        if (pdus == null || format == null) {
            Logger.w("SmsListener", "PDUs or format is null");
            return null;
        }

        MessageContainer[] messages = new MessageContainer[pdus.length];
        for (int i = 0; i < messages.length; ++i) {
            MessageContainer message = messages[i] = new MessageContainer(pdus[i], format);

            Logger.d("SmsListener", "Message address: " + message.getAddressFrom());
            Logger.d("SmsListener", "Message body: " + message.getBody());
        }

        Logger.i("SmsListener", "Receive messages");

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

        baseIntent.putStringArrayListExtra(AppConstants.EXTRA_GOT_SMS, intentData);

        Intent smsHandlerIntent = new Intent(baseIntent);
        smsHandlerIntent.setClass(context, SmsDbHandler.class);
        context.startService(smsHandlerIntent);

        Intent smsAdderIntent = new Intent(baseIntent);
        smsAdderIntent.setClass(context, SmsApiSender.class);
        context.startService(smsAdderIntent);
    }
}
