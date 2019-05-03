package life.andre.sms487.services.smsHandler;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageStorage;
import life.andre.sms487.network.SmsApi;

public class SmsRequestListener implements SmsApi.RequestHandledListener {
    private MessageStorage messageStorage;

    public SmsRequestListener(MessageStorage messageStorage) {
        this.messageStorage = messageStorage;
    }

    @Override
    public void onSuccess(long dbId) {
        Logger.i("SmsRequestListener", "Successfully sent SMS " + dbId);

        MarkSmsSentParams params = new MarkSmsSentParams(dbId, messageStorage);
        new MarkSmsSentAction().execute(params);
    }

    @Override
    public void onError(long errorMessage, String dbId) {
        Logger.w("SmsRequestListener", "Error with SMS " + dbId + ": " + errorMessage);
    }
}
