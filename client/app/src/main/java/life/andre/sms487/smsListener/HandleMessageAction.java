package life.andre.sms487.smsListener;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Telephony;

import life.andre.sms487.messages.PduConverter;
import life.andre.sms487.system.AppConstants;
import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageContainer;
import life.andre.sms487.services.smsHandler.SmsHandler;

class HandleMessageAction extends AsyncTask<HandleMessageParams, Void, Void> {
    private static PduConverter converter = new PduConverter();

    @Override
    protected Void doInBackground(HandleMessageParams... params) {
        if (params.length == 0) {
            Logger.w("HandleMessageAction", "Params length is 0");
            return null;
        }
        HandleMessageParams mainParams = params[0];

        List<MessageContainer> messages = extractMessages(mainParams.intent);
        if (messages != null) {
            sendMessagesToHandler(mainParams.context, messages);
        }

        return null;
    }

    private List<MessageContainer> extractMessages(Intent intent) {
        String action = intent.getAction();
        if (action == null || !action.equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            return null;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            Logger.w("HandleMessageAction", "Bundle is null");
            return null;
        }

        Object[] pdus = (Object[]) bundle.get("pdus");
        String format = bundle.getString("format");
        if (pdus == null || format == null) {
            Logger.w("HandleMessageAction", "PDUs or format is null");
            return null;
        }

        List<MessageContainer> messages = converter.convert(pdus, format);

        Logger.i("HandleMessageAction", "Receive messages");

        return messages;
    }

    private void sendMessagesToHandler(Context context, List<MessageContainer> messages) {
        Intent baseIntent = new Intent();

        ArrayList<String> intentData = new ArrayList<>();
        for (MessageContainer message : messages) {
            String messageJson = message.toString();
            intentData.add(messageJson);
        }

        baseIntent.putStringArrayListExtra(AppConstants.EXTRA_GOT_SMS, intentData);

        Intent smsAdderIntent = new Intent(baseIntent);
        smsAdderIntent.setClass(context, SmsHandler.class);
        context.startService(smsAdderIntent);
    }
}
