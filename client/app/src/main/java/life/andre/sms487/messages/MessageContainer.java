package life.andre.sms487.messages;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import life.andre.sms487.logging.Logger;

public class MessageContainer {
    public static final String TAG = MessageContainer.class.getSimpleName();

    private final String messageType;
    private final String addressFrom;
    private final String dateTime;
    private final String smsCenterDateTime;
    private final String body;
    private final boolean isSent;
    private final long dbId;

    @Nullable
    public static MessageContainer createFromJson(@NonNull String messageJson) {
        try {
            JSONObject obj = new JSONObject(messageJson);

            String messageType = obj.getString("message_type");
            String addressFrom = obj.getString("address_from");
            String dateTime = obj.getString("date_time");
            String smsCenterDateTime = obj.getString("sms_date_time");
            String body = obj.getString("body");
            boolean isSent = obj.getBoolean("is_sent");
            long dbId = obj.getInt("db_id");

            return new MessageContainer(messageType, addressFrom, dateTime, smsCenterDateTime, body, isSent, dbId);
        } catch (JSONException e) {
            Logger.w(TAG, e.toString());
            e.printStackTrace();
        }

        return null;
    }

    @NonNull
    public static MessageContainer createFromMessageEntry(@NonNull MessageStorage.Message messageEntry) {
        return new MessageContainer(
                messageEntry.messageType,
                messageEntry.addressFrom,
                messageEntry.dateTime,
                messageEntry.smsCenterDateTime,
                messageEntry.body,
                messageEntry.isSent,
                messageEntry.id
        );
    }

    public MessageContainer(String messageType, String addressFrom, String dateTime, String smsCenterDateTime, String body, boolean isSent, long dbId) {
        this.messageType = messageType;
        this.addressFrom = addressFrom;
        this.dateTime = dateTime;
        this.smsCenterDateTime = smsCenterDateTime;
        this.body = body;
        this.isSent = isSent;
        this.dbId = dbId;
    }

    public MessageContainer(String messageType, String addressFrom, String dateTime, String smsCenterDateTime, String body) {
        this.messageType = messageType;
        this.addressFrom = addressFrom;
        this.dateTime = dateTime;
        this.smsCenterDateTime = smsCenterDateTime;
        this.body = body;
        this.isSent = false;
        this.dbId = 0;
    }

    @Nullable
    public String getAsJson() {
        try {
            JSONObject obj = new JSONObject();

            obj.put("message_type", messageType);
            obj.put("address_from", addressFrom);
            obj.put("date_time", dateTime);
            obj.put("sms_date_time", smsCenterDateTime);
            obj.put("body", body);
            obj.put("is_sent", isSent);
            obj.put("db_id", dbId);

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

    public long getDbId() {
        return dbId;
    }

    public boolean isSent() {
        return isSent;
    }

    @NonNull
    public String getMessageType() {
        return messageType;
    }
}
