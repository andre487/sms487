package life.andre.sms487.messages;

import android.telephony.SmsMessage;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import life.andre.sms487.network.ServerApi;
import life.andre.sms487.utils.DateUtil;

public class PduConverter {
    @NonNull
    public static List<MessageContainer> convert(@NonNull Object[] pdus, String format) {
        Map<Pair<String, String>, List<SmsMessage>> messageTable = getMessageTable(pdus, format);

        return getMessageContainers(messageTable);
    }

    @NonNull
    private static Map<Pair<String, String>, List<SmsMessage>> getMessageTable(
            @NonNull Object[] pdus, String format
    ) {
        Map<Pair<String, String>, List<SmsMessage>> messageTable = new HashMap<>();

        for (Object pdu : pdus) {
            byte[] pduBytes = (byte[]) pdu;

            SmsMessage message = SmsMessage.createFromPdu(pduBytes, format);
            String smsCenterDateTime = DateUtil.formatDate(message.getTimestampMillis());

            String tel = message.getOriginatingAddress();
            Pair<String, String> key = new Pair<>(tel, smsCenterDateTime);

            List<SmsMessage> telMessages = messageTable.get(key);
            if (telMessages == null) {
                telMessages = new ArrayList<>();
                messageTable.put(key, telMessages);
            }

            telMessages.add(message);
        }

        return messageTable;
    }

    @NonNull
    private static List<MessageContainer> getMessageContainers(
            @NonNull Map<Pair<String, String>, List<SmsMessage>> messageTable
    ) {
        List<MessageContainer> messageContainers = new ArrayList<>();

        for (Map.Entry<Pair<String, String>, List<SmsMessage>> entry : messageTable.entrySet()) {
            Pair<String, String> key = entry.getKey();
            List<SmsMessage> messages = entry.getValue();

            String tel = key.first;
            String smsCenterDateTime = key.second;
            StringBuilder fullTextBuilder = new StringBuilder();

            String dateTime = DateUtil.nowFormatted();

            for (SmsMessage message : messages) {
                String messageBody = message.getMessageBody();
                fullTextBuilder.append(messageBody);
            }

            messageContainers.add(new MessageContainer(
                    ServerApi.MESSAGE_TYPE_SMS,
                    tel, dateTime, smsCenterDateTime, fullTextBuilder.toString()
            ));
        }

        return messageContainers;
    }
}
