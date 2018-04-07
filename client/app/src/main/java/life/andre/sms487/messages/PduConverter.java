package life.andre.sms487.messages;

import android.support.annotation.NonNull;
import android.telephony.SmsMessage;
import android.util.Pair;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import life.andre.sms487.logging.Logger;

public class PduConverter {
    private SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm Z",
            new Locale("UTC")
    );

    public PduConverter() {
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public List<MessageContainer> convert(Object[] pdus, String format) {
        Map<Pair<String, String>, List<SmsMessage>> messageTable = getMessageTable(pdus, format);

        return getMessageContainers(messageTable);
    }

    @NonNull
    private Map<Pair<String, String>, List<SmsMessage>> getMessageTable(
            Object[] pdus, String format
    ) {
        Map<Pair<String, String>, List<SmsMessage>> messageTable = new HashMap<>();

        for (Object pdu : pdus) {
            byte[] pduBytes = (byte[]) pdu;

            SmsMessage message = SmsMessage.createFromPdu(pduBytes, format);

            String tel = message.getOriginatingAddress();
            String dateTime = dateFormat.format(new Date(message.getTimestampMillis()));
            Pair<String, String> key = new Pair<>(tel, dateTime);

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
    private List<MessageContainer> getMessageContainers(
            Map<Pair<String, String>, List<SmsMessage>> messageTable
    ) {
        List<MessageContainer> messageContainers = new ArrayList<>();

        for (Map.Entry<Pair<String, String>, List<SmsMessage>> entry : messageTable.entrySet()) {
            Pair<String, String> key = entry.getKey();
            List<SmsMessage> messages = entry.getValue();

            String tel = key.first;
            String dateTime = key.second;
            StringBuilder fullTextBuilder = new StringBuilder();

            Logger.d(
                    "PduConverter",
                    "Handle message from: " + tel + ", dateTime: " + dateTime
            );

            for (SmsMessage message : messages) {
                String messageBody = message.getMessageBody();
                fullTextBuilder.append(messageBody);

                Logger.d("PduConverter", "Handle message body: " + messageBody);
            }

            MessageContainer container = new MessageContainer(
                    tel, dateTime, fullTextBuilder.toString()
            );
            messageContainers.add(container);
        }

        return messageContainers;
    }
}