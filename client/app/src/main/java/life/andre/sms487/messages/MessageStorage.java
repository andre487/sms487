package life.andre.sms487.messages;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class MessageStorage {
    @Dao
    public interface MessageDao {
        @Query("SELECT * FROM message ORDER BY id DESC LIMIT 30")
        List<Message> getTail();

        @Query("SELECT * FROM message WHERE is_sent == 0 ORDER BY id ASC")
        List<Message> getNotSent();

        @Insert
        long insert(Message message);

        @Query("UPDATE message SET is_sent=1 WHERE id=:insertId")
        void markSent(long insertId);
    }

    @SuppressWarnings("WeakerAccess")
    @Entity
    public static class Message {
        @PrimaryKey(autoGenerate = true)
        public int id;

        @ColumnInfo(name = "address_from")
        public String addressFrom;

        @ColumnInfo(name = "device_id")
        public String deviceId;

        @ColumnInfo(name = "date_time", index = true)
        public String dateTime;

        @ColumnInfo(name = "body")
        public String body;

        @ColumnInfo(name = "is_sent", index = true)
        public boolean isSent = false;
    }

    @SuppressWarnings("WeakerAccess")
    @Database(entities = {Message.class}, version = 1, exportSchema = false)
    public static abstract class AppDatabase extends RoomDatabase {
        public abstract MessageDao messageDao();
    }

    private MessageDao messageDao;

    public MessageStorage(Context context) {
        AppDatabase db = Room.databaseBuilder(
                context, AppDatabase.class,
                "messages-db"
        ).build();
        messageDao = db.messageDao();
    }

    public long addMessage(MessageContainer message) {
        Message entry = new Message();

        entry.deviceId = message.getDeviceId();
        entry.addressFrom = message.getAddressFrom();
        entry.dateTime = message.getDateTime();
        entry.body = message.getBody();

        return messageDao.insert(entry);
    }

    public void markSent(long insertId) {
        messageDao.markSent(insertId);
    }

    public List<MessageContainer> getMessagesTail() {
        List<Message> messageEntries = messageDao.getTail();
        List<MessageContainer> messages = new ArrayList<>();

        for (Message messageEntry : messageEntries) {
            messages.add(
                    new MessageContainer(
                            messageEntry.deviceId,
                            messageEntry.addressFrom,
                            messageEntry.dateTime,
                            messageEntry.body,
                            messageEntry.isSent,
                            messageEntry.id
                    )
            );
        }

        return messages;
    }

    public List<MessageContainer> getNotSentMessages() {
        List<Message> messageEntries = messageDao.getNotSent();
        List<MessageContainer> messages = new ArrayList<>();

        for (Message messageEntry : messageEntries) {
            messages.add(
                    new MessageContainer(
                            messageEntry.deviceId,
                            messageEntry.addressFrom,
                            messageEntry.dateTime,
                            messageEntry.body,
                            messageEntry.isSent,
                            messageEntry.id
                    )
            );
        }

        return messages;
    }
}
