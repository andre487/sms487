package com.example.andre487.sms487.messages;

import android.telephony.SmsMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

@SuppressWarnings("WeakerAccess")
public class MessageContainer {
    private String addressFrom;
    private String dateTime;
    private String body;

    public MessageContainer(String addressFrom, String dateTime, String body) {
        this.addressFrom = addressFrom;
        this.dateTime = dateTime;
        this.body = body;
    }

    public MessageContainer(Object pdu, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm Z",
                new Locale("UTC")
        );
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        byte[] bPdu = (byte[]) pdu;
        SmsMessage message = SmsMessage.createFromPdu(bPdu, format);
        Date messageDate = new Date(message.getTimestampMillis());

        this.addressFrom = message.getOriginatingAddress();
        this.dateTime = dateFormat.format(messageDate);
        this.body = message.getMessageBody();
    }

    public String toString() {
        try {
            JSONObject obj = new JSONObject();

            obj.put("address_from", addressFrom);
            obj.put("date_time", dateTime);
            obj.put("body", body);

            return obj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getAddressFrom() {
        return addressFrom;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getBody() {
        return body;
    }
}
