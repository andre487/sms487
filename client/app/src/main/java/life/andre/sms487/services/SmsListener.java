package life.andre.sms487.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageContainer;
import life.andre.sms487.messages.PduConverter;
import life.andre.sms487.network.ServerApi;
import life.andre.sms487.settings.AppSettings;
import life.andre.sms487.utils.BgTask;

public class SmsListener extends BroadcastReceiver {
    public static final String TAG = "SmsListener";

    @Override
    public void onReceive(@NonNull Context ctx, @NonNull Intent intent) {
        BgTask.run(() -> {
            handleMessagesReceive(intent);
            return null;
        });
    }

    private void handleMessagesReceive(@NonNull Intent intent) {
        boolean needSend = AppSettings.getInstance().getNeedSendSms();
        if (!needSend) {
            return;
        }

        List<MessageContainer> messages = extractMessages(intent);
        if (messages == null) {
            return;
        }

        ServerApi serverApi = ServerApi.getInstance();
        for (MessageContainer message : messages) {
            serverApi.addMessage(message);
        }
    }

    @Nullable
    private List<MessageContainer> extractMessages(@NonNull Intent intent) {
        String action = intent.getAction();
        if (action == null || !action.equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            return null;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            Logger.w(TAG, "Bundle is null");
            return null;
        }

        Object[] pdus = (Object[]) bundle.get("pdus");
        String format = bundle.getString("format");
        if (pdus == null || format == null) {
            Logger.w(TAG, "PDUs or format is null");
            return null;
        }

        return PduConverter.convert(pdus, format);
    }
}
