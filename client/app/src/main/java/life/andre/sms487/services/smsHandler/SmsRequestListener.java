package life.andre.sms487.services.smsHandler;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageStorage;
import life.andre.sms487.network.SmsApi;

@Deprecated
public class SmsRequestListener implements SmsApi.RequestHandledListener {
    private final MessageStorage messageStorage;
    private String logTag = "SmsRequestListener";

    public SmsRequestListener(MessageStorage messageStorage, String tag) {
        this.messageStorage = messageStorage;
        logTag += ":" + tag;
    }

    @Override
    public void onSuccess(long dbId) {
        Logger.i(logTag, "Successfully sent SMS " + dbId);

        MarkSmsSentParams params = new MarkSmsSentParams(dbId, messageStorage);
        new MarkSmsSentAction().execute(params);
    }

    @Override
    public void onError(long dbId, String errorMessage) {
        Logger.e(logTag, "Error with SMS " + dbId + ": " + errorMessage);
    }
}
