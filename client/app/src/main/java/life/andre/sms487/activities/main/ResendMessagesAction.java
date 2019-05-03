package life.andre.sms487.activities.main;

import android.os.AsyncTask;

import java.util.List;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageContainer;

public class ResendMessagesAction extends AsyncTask<ResendMessagesParams, Void, Void> {
    @Override
    protected Void doInBackground(ResendMessagesParams... params) {
        if (params.length == 0) {
            Logger.w("MarkSmsSentAction", "Params length is 0");
            return null;
        }
        ResendMessagesParams mainParams = params[0];

        Logger.i("ResendMessagesAction", "Resend messages");

        List<MessageContainer> messages = mainParams.messageStorage.getNotSentMessages();

        for (MessageContainer message : messages) {
            mainParams.smsApi.addSms(
                    message.getDeviceId(),
                    message.getDateTime(),
                    message.getAddressFrom(),
                    message.getBody(),
                    message.getDbId()
            );
        }

        return null;
    }
}
