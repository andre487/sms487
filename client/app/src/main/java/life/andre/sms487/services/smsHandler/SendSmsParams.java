package life.andre.sms487.services.smsHandler;

import android.content.Intent;

import life.andre.sms487.messages.MessageStorage;
import life.andre.sms487.network.SmsApi;

class SendSmsParams {
    Intent intent;
    SmsApi smsApi;
    MessageStorage messageStorage;
    String deviceId;

    SendSmsParams(Intent intent, SmsApi smsApi, MessageStorage messageStorage, String deviceId) {
        this.intent = intent;
        this.smsApi = smsApi;
        this.messageStorage = messageStorage;
        this.deviceId = deviceId;
    }
}
