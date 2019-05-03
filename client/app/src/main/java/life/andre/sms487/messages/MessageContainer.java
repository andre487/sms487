package life.andre.sms487.messages;

import org.json.JSONException;
import org.json.JSONObject;

import life.andre.sms487.logging.Logger;

public class MessageContainer {
    private String addressFrom;
    private String dateTime;
    private String body;
    private boolean isSent;

    @SuppressWarnings("WeakerAccess")
    public MessageContainer(String addressFrom, String dateTime, String body, boolean isSent) {
        this.addressFrom = addressFrom;
        this.dateTime = dateTime;
        this.body = body;
        this.isSent = isSent;
    }

    public MessageContainer(String addressFrom, String dateTime, String body) {
        this(addressFrom, dateTime, body, false);
    }

    public String toString() {
        try {
            JSONObject obj = new JSONObject();

            obj.put("address_from", addressFrom);
            obj.put("date_time", dateTime);
            obj.put("body", body);
            obj.put("is_sent", isSent);

            return obj.toString();
        } catch (JSONException e) {
            Logger.w("MessageContainer", e.toString());
            e.printStackTrace();
        }

        return null;
    }

    public String getAddressFrom() {
        return addressFrom;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getBody() {
        return body;
    }

    public boolean isSent() {
        return isSent;
    }
}
