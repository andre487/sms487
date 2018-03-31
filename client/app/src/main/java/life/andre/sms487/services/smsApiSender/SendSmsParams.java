package life.andre.sms487.services.smsApiSender;

import android.content.Intent;

import life.andre.sms487.network.SmsApi;

class SendSmsParams {
    Intent intent;
    SmsApi smsApi;
    String deviceId;

    SendSmsParams(Intent intent, SmsApi smsApi, String deviceId) {
        this.intent = intent;
        this.smsApi = smsApi;
        this.deviceId = deviceId;
    }
}
