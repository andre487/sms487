package life.andre.sms487.services.smsHandler;

import life.andre.sms487.messages.MessageStorage;

class MarkSmsSentParams {
    long dbId;
    MessageStorage messageStorage;

    MarkSmsSentParams(long dbId, MessageStorage messageStorage) {
        this.dbId = dbId;
        this.messageStorage = messageStorage;
    }
}
