package life.andre.sms487.activities.main;

import life.andre.sms487.messages.MessageStorage;

class GetMessagesParams {
    MessageStorage messageStorage;

    GetMessagesParams(MessageStorage messageStorage) {
        this.messageStorage = messageStorage;
    }
}
