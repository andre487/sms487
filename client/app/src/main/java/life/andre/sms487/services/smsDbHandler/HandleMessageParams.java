package life.andre.sms487.services.smsDbHandler;

import android.content.Intent;

import life.andre.sms487.messages.MessageStorage;

class HandleMessageParams {
    Intent intent;
    MessageStorage messageStorage;

    HandleMessageParams(Intent intent, MessageStorage messageStorage) {
        this.intent = intent;
        this.messageStorage = messageStorage;
    }
}
