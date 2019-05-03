package life.andre.sms487.activities.main;

import life.andre.sms487.messages.MessageStorage;
import life.andre.sms487.network.SmsApi;

class ResendMessagesParams {
    MessageStorage messageStorage;
    SmsApi smsApi;

    ResendMessagesParams(MessageStorage messageStorage, SmsApi smsApi) {
        this.messageStorage = messageStorage;
        this.smsApi = smsApi;
    }
}
