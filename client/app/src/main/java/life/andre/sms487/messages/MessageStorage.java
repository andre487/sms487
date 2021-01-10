package life.andre.sms487.messages;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.ArrayList;
import java.util.List;

import life.andre.sms487.logging.Logger;

public class MessageStorage {
    public static final String TAG = "MessageStorage";

    private final MessageDao dao;

    public MessageStorage(@NonNull Context ctx) {
        dao = Room.databaseBuilder(ctx, MessageDatabase.class, "messages").build().messageDao();
    }

    public long addMessage(@NonNull MessageContainer message) {
        return dao.insert(Message.createFromMessageContainer(message));
    }

    @NonNull
    public List<MessageContainer> getMessagesTail() {
        List<Message> messageEntries = dao.getTail();
        List<MessageContainer> messages = new ArrayList<>();

        for (Message messageEntry : messageEntries) {
            messages.add(MessageContainer.createFromMessageEntry(messageEntry));
        }

        return messages;
    }

    @NonNull
    public List<MessageContainer> getNotSentMessages() {
        List<Message> messageEntries = dao.getNotSent();
        List<MessageContainer> messages = new ArrayList<>();

        for (Message messageEntry : messageEntries) {
            messages.add(MessageContainer.createFromMessageEntry(messageEntry));
        }

        return messages;
    }

    public void markSent(long insertId) {
        dao.markSent(insertId);
    }

    public void deleteOld() {
        int oldCount = dao.deleteOld();
        Logger.i(TAG, "Old messages deleted: " + oldCount);
    }

    @Entity
    public static class Message {
        @PrimaryKey(autoGenerate = true)
        public int id;

        @Nullable
        public String messageType;

        @Nullable
        public String addressFrom;

        @Nullable
        @ColumnInfo(index = true)
        public String dateTime;

        @Nullable
        public String smsCenterDateTime;

        @Nullable
        public String body;

        @ColumnInfo(index = true, defaultValue = "0")
        public boolean isSent;

        @NonNull
        public static Message createFromMessageContainer(@NonNull MessageContainer messageContainer) {
            Message message = new Message();

            message.messageType = messageContainer.getMessageType();
            message.addressFrom = messageContainer.getAddressFrom();
            message.dateTime = messageContainer.getDateTime();
            message.smsCenterDateTime = messageContainer.getSmsCenterDateTime();
            message.body = messageContainer.getBody();

            return message;
        }
    }

    @Dao
    public interface MessageDao {
        @NonNull
        @Query("SELECT * FROM message ORDER BY id DESC LIMIT 5")
        List<Message> getTail();

        @NonNull
        @Query("SELECT * FROM message WHERE isSent == 0 ORDER BY id DESC")
        List<Message> getNotSent();

        @Insert
        long insert(Message message);

        @Query("UPDATE message SET isSent=1 WHERE id=:insertId")
        void markSent(long insertId);

        @Query("DELETE FROM message WHERE id IN (SELECT id FROM message WHERE (isSent == 1 OR dateTime < strftime('%Y-%m-%d 00:00', 'now', 'utc', '-2 days')) ORDER BY id DESC LIMIT 5,100000)")
        int deleteOld();
    }

    @Database(entities = {Message.class}, version = 1, exportSchema = false)
    public static abstract class MessageDatabase extends RoomDatabase {
        public abstract MessageDao messageDao();
    }
}
