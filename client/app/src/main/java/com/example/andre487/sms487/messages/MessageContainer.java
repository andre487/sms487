package com.example.andre487.sms487.messages;

import android.telephony.SmsMessage;

class MessageContainer {
    private String addressFrom;
    private String body;

    MessageContainer(Object pdu, String format) {
        byte[] bPdu = (byte[]) pdu;
        SmsMessage message = SmsMessage.createFromPdu(bPdu, format);

        this.addressFrom = message.getOriginatingAddress();
        this.body = message.getMessageBody();
    }

    public String getAddressFrom() {
        return addressFrom;
    }

    public String getBody() {
        return body;
    }
}
