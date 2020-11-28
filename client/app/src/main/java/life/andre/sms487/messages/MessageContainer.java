package life.andre.sms487.messages;

import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import life.andre.sms487.logging.Logger;

public class MessageContainer {
    private final String deviceId;
    private final String addressFrom;
    private final String dateTime;
    private final String body;
    private final boolean isSent;
    private final long dbId;

    public MessageContainer(
            String deviceId, String addressFrom,
            String dateTime, String body, boolean isSent, long dbId
    ) {
        this.deviceId = deviceId;
        this.addressFrom = addressFrom;
        this.dateTime = dateTime;
        this.body = body;
        this.isSent = isSent;
        this.dbId = dbId;
    }

    public MessageContainer(String addressFrom, String dateTime, String body) {
        this(Build.MODEL, addressFrom, dateTime, body, false, 0);
    }

    public String getAsString() {
        try {
            JSONObject obj = new JSONObject();

            obj.put("device_id", deviceId);
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

    public String getDeviceId() {
        return deviceId;
    }

    public long getDbId() {
        return dbId;
    }
}
