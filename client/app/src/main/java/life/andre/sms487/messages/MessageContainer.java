package life.andre.sms487.messages;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MessageContainer {
    private final String messageType;
    private final String addressFrom;
    private final String dateTime;
    private final String smsCenterDateTime;
    private final String body;
    private final boolean isSent;
    private long dbId;

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

    public void setDbId(long dbId) {
        this.dbId = dbId;
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
