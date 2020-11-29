package life.andre.sms487.services.notificationListener;

import life.andre.sms487.messages.MessageStorage;
import life.andre.sms487.network.SmsApi;

public class SendNotificationParams {
    SmsApi smsApi;
    MessageStorage messageStorage;

    String appLabel;
    long postTime;
    String text;
    String deviceId;

    SendNotificationParams(
            SmsApi smsApi, MessageStorage messageStorage, String appLabel, long postTime,
            String text, String deviceId
    ) {
        this.smsApi = smsApi;
        this.messageStorage = messageStorage;
        this.appLabel = appLabel;
        this.postTime = postTime;
        this.text = text;
        this.deviceId = deviceId;
    }
}
