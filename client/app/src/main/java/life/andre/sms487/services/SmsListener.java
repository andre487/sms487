package life.andre.sms487.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Telephony;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageContainer;
import life.andre.sms487.messages.PduConverter;
import life.andre.sms487.system.AppConstants;
import life.andre.sms487.utils.AsyncTaskUtil;

public class SmsListener extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        new HandleMessageAction().execute(new HandleMessageParams(context, intent));
    }

    static class HandleMessageParams {
        final Context context;
        final Intent intent;

        HandleMessageParams(Context context, Intent intent) {
            this.context = context;
            this.intent = intent;
        }
    }

    static class HandleMessageAction extends AsyncTask<HandleMessageParams, Void, Void> {
        private static final String logTag = "HandleMessageAction";
        private final PduConverter converter = new PduConverter();

        @Nullable
        @Override
        protected Void doInBackground(@NonNull HandleMessageParams... params) {
            HandleMessageParams mainParams = AsyncTaskUtil.getParams(params, logTag);
            if (mainParams == null) {
                return null;
            }

            List<MessageContainer> messages = extractMessages(mainParams.intent);
            if (messages != null) {
                sendMessagesToHandler(mainParams.context, messages);
            }

            return null;
        }

        @Nullable
        private List<MessageContainer> extractMessages(@NonNull Intent intent) {
            String action = intent.getAction();
            if (action == null || !action.equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
                return null;
            }

            Bundle bundle = intent.getExtras();
            if (bundle == null) {
                Logger.w(logTag, "Bundle is null");
                return null;
            }

            Object[] pdus = (Object[]) bundle.get("pdus");
            String format = bundle.getString("format");
            if (pdus == null || format == null) {
                Logger.w(logTag, "PDUs or format is null");
                return null;
            }

            return converter.convert(pdus, format);
        }

        private void sendMessagesToHandler(@NonNull Context context, @NonNull List<MessageContainer> messages) {
            Intent baseIntent = new Intent();

            ArrayList<String> intentData = new ArrayList<>();
            for (MessageContainer message : messages) {
                String messageJson = message.getAsJson();
                if (messageJson != null) {
                    intentData.add(messageJson);
                }
            }

            baseIntent.putStringArrayListExtra(AppConstants.EXTRA_GOT_SMS, intentData);

            Intent smsAdderIntent = new Intent(baseIntent);
            smsAdderIntent.setClass(context, SmsHandler.class);
            context.startService(smsAdderIntent);
        }
    }
}
