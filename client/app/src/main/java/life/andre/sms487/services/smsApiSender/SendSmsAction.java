package life.andre.sms487.services.smsApiSender;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import life.andre.sms487.system.AppConstants;
import life.andre.sms487.logging.Logger;
import life.andre.sms487.messageStorage.MessageContainer;
import life.andre.sms487.network.SmsApi;

class SendSmsAction extends AsyncTask<SendSmsParams, Void, Void> {
    @Override
    protected Void doInBackground(SendSmsParams... params) {
        if (params.length == 0) {
            Logger.w("SendSmsAction", "Params length is 0");
            return null;
        }
        SendSmsParams mainParams = params[0];

        ArrayList<String> intentData = mainParams.intent.getStringArrayListExtra(
                AppConstants.EXTRA_GOT_SMS
        );

        if (intentData == null) {
            Logger.w("SendSmsAction", "Intent data is null");
            return null;
        }

        handleIntentData(mainParams.smsApi, mainParams.deviceId, intentData);

        return null;
    }

    private void handleIntentData(SmsApi smsApi, String deviceId, ArrayList<String> intentData) {
        for (MessageContainer message : extractMessages(intentData)) {
            smsApi.addSms(
                    deviceId,
                    message.getDateTime(),
                    message.getAddressFrom(),
                    message.getBody()
            );
        }
    }

    private ArrayList<MessageContainer> extractMessages(ArrayList<String> intentData) {
        ArrayList<MessageContainer> data = new ArrayList<>();

        for (String messageJson : intentData) {
            Logger.d("SendSmsAction", "Got message: " + messageJson);

            try {
                JSONObject obj = new JSONObject(messageJson);

                String addressFrom = (String) obj.get("address_from");
                String dateTime = (String) obj.get("date_time");
                String body = (String) obj.get("body");

                MessageContainer message = new MessageContainer(addressFrom, dateTime, body);
                data.add(message);
            } catch (JSONException e) {
                Logger.w("SendSmsAction", e.toString());
                e.printStackTrace();
            }
        }

        return data;
    }
}
