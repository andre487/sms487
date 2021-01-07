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

public class MessageStorage {
    private final MessageDao messageDao;

    @Database(entities = {Message.class}, version = 1, exportSchema = false)
    public static abstract class AppDatabase extends RoomDatabase {
        public abstract MessageDao messageDao();
    }

    public MessageStorage(@NonNull Context context) {
        messageDao = Room.databaseBuilder(context, AppDatabase.class, "messages").build().messageDao();
    }

    public long addMessage(@NonNull MessageContainer message) {
        return messageDao.insert(Message.createFromMessageContainer(message));
    }

    public void markSent(long insertId) {
        messageDao.markSent(insertId);
    }

    @NonNull
    public List<MessageContainer> getMessagesTail() {
        List<Message> messageEntries = messageDao.getTail();
        List<MessageContainer> messages = new ArrayList<>();

        for (Message messageEntry : messageEntries) {
            messages.add(MessageContainer.createFromMessageEntry(messageEntry));
        }

        return messages;
    }

    @NonNull
    public List<MessageContainer> getNotSentMessages() {
        List<Message> messageEntries = messageDao.getNotSent();
        List<MessageContainer> messages = new ArrayList<>();

        for (Message messageEntry : messageEntries) {
            messages.add(MessageContainer.createFromMessageEntry(messageEntry));
        }

        return messages;
    }

    public int deleteOldMessages() {
        return messageDao.deleteOld();
    }

    @Dao
    public interface MessageDao {
        @Query("SELECT * FROM message ORDER BY id DESC LIMIT 6")
        List<Message> getTail();

        @Query("SELECT * FROM message WHERE is_sent == 0 ORDER BY id DESC")
        List<Message> getNotSent();

        @Insert
        long insert(Message message);

        @Query("UPDATE message SET is_sent=1 WHERE id=:insertId")
        void markSent(long insertId);

        @Query("DELETE FROM message WHERE date_time < strftime('%Y-%m-%d 00:00', 'now', 'utc', '-2 days') OR date_time IS NULL OR date_time == \"\"")
        int deleteOld();
    }

    @Entity
    public static class Message {
        @PrimaryKey(autoGenerate = true)
        public int id;

        @Nullable
        @ColumnInfo(name = "message_type")
        public String messageType;

        @Nullable
        @ColumnInfo(name = "address_from")
        public String addressFrom;

        @Nullable
        @ColumnInfo(name = "date_time", index = true)
        public String dateTime;

        @ColumnInfo(name = "sms_date_time")
        public String smsCenterDateTime;

        @Nullable
        @ColumnInfo(name = "body")
        public String body;

        @ColumnInfo(name = "is_sent", index = true, defaultValue = "0")
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
}
