package life.andre.sms487.messages;

import android.os.Build;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import life.andre.sms487.logging.Logger;

public class MessageContainer {
    private final String deviceId;
    private final String addressFrom;
    private final String dateTime;
    private final String smsCenterDateTime;
    private final String body;
    private final boolean isSent;
    private final long dbId;

    public MessageContainer(
            String deviceId, String addressFrom,
            String dateTime, String smsCenterDateTime,
            String body, boolean isSent, long dbId
    ) {
        this.deviceId = deviceId;
        this.addressFrom = addressFrom;
        this.dateTime = dateTime;
        this.smsCenterDateTime = smsCenterDateTime;
        this.body = body;
        this.isSent = isSent;
        this.dbId = dbId;
    }

    @Nullable
    public static MessageContainer createFromJson(String messageJson) {
        try {
            JSONObject obj = new JSONObject(messageJson);

            String addressFrom = (String) obj.get("address_from");
            String dateTime = (String) obj.get("date_time");
            String smsCenterDateTime = (String) obj.get("sms_date_time");
            String body = (String) obj.get("body");

            return new MessageContainer(addressFrom, dateTime, smsCenterDateTime, body);
        } catch (JSONException e) {
            Logger.w("SendSmsAction", e.toString());
            e.printStackTrace();
        }

        return null;
    }

    public MessageContainer(String addressFrom, String dateTime, String smsCenterDateTime, String body) {
        this(Build.MODEL, addressFrom, dateTime, smsCenterDateTime, body, false, 0);
    }

    @Nullable
    public String getAsJson() {
        try {
            JSONObject obj = new JSONObject();

            obj.put("device_id", deviceId);
            obj.put("address_from", addressFrom);
            obj.put("date_time", dateTime);
            obj.put("sms_date_time", smsCenterDateTime);
            obj.put("body", body);
            obj.put("is_sent", isSent);

            return obj.toString();
        } catch (JSONException e) {
            Logger.w("MessageContainer", e.toString());
            e.printStackTrace();
        }

        return null;
    }

    @Nullable
    public String getAddressFrom() {
        return addressFrom;
    }

    @Nullable
    public String getDateTime() {
        return dateTime;
    }

    @Nullable
    public String getSmsCenterDateTime() {
        return smsCenterDateTime;
    }

    @Nullable
    public String getBody() {
        return body;
    }

    @Nullable
    public String getDeviceId() {
        return deviceId;
    }

    public long getDbId() {
        return dbId;
    }

    public boolean isSent() {
        return isSent;
    }
}
