package life.andre.sms487.services.smsDbHandler;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import life.andre.sms487.AppConstants;
import life.andre.sms487.logging.Logger;
import life.andre.sms487.messageStorage.MessageContainer;
import life.andre.sms487.messageStorage.MessageStorage;

class HandleMessageAction extends AsyncTask<HandleMessageParams, Void, Void> {
    @Override
    protected Void doInBackground(HandleMessageParams... params) {
        if (params.length == 0) {
            Logger.w("SmsDbHandler", "Params length is 0");
            return null;
        }

        HandleMessageParams mainParams = params[0];
        List<String> intentData = mainParams.intent.getStringArrayListExtra(
                AppConstants.EXTRA_GOT_SMS
        );

        if (intentData == null) {
            Logger.w("SmsDbHandler", "Intent data is null");
            return null;
        }

        handleIntentData(intentData, mainParams.messageStorage);

        return null;
    }


    private void handleIntentData(List<String> intentData, MessageStorage messageStorage) {
        List<MessageContainer> messages = extractMessages(intentData);

        messageStorage.addMessages(messages);
    }

    private List<MessageContainer> extractMessages(List<String> intentData) {
        List<MessageContainer> data = new ArrayList<>();

        for (String messageJson : intentData) {
            Logger.d("SmsDbHandler", "Got message: " + messageJson);

            try {
                JSONObject obj = new JSONObject(messageJson);

                String addressFrom = (String) obj.get("address_from");
                String dateTime = (String) obj.get("date_time");
                String body = (String) obj.get("body");

                MessageContainer message = new MessageContainer(addressFrom, dateTime, body);
                data.add(message);
            } catch (JSONException e) {
                Logger.w("HandleMessageAction", e.toString());
                e.printStackTrace();
            }
        }

        return data;
    }
}
