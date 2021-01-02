package life.andre.sms487.messages;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class MessageStorage {
    private final MessageDao messageDao;

    @Database(entities = {Message.class}, version = 2, exportSchema = false)
    public static abstract class AppDatabase extends RoomDatabase {
        public abstract MessageDao messageDao();
    }

    public MessageStorage(@NonNull Context context) {
        RoomDatabase.Builder<AppDatabase> builder = Room.databaseBuilder(context, AppDatabase.class, "messages-db");

        builder.addMigrations(
                new Migration(1, 2) {
                    @Override
                    public void migrate(@NonNull SupportSQLiteDatabase database) {
                        database.execSQL("ALTER TABLE message ADD COLUMN sms_date_time TEXT DEFAULT \"\"");
                    }
                }
        );

        AppDatabase db = builder.build();
        messageDao = db.messageDao();
    }

    public long addMessage(@NonNull MessageContainer message) {
        return messageDao.insert(Message.fromMessageContainer(message));
    }

    public void markSent(long insertId) {
        messageDao.markSent(insertId);
    }

    @NonNull
    public List<MessageContainer> getMessagesTail() {
        List<Message> messageEntries = messageDao.getTail();
        List<MessageContainer> messages = new ArrayList<>();

        for (Message messageEntry : messageEntries) {
            messages.add(
                    new MessageContainer(
                            messageEntry.deviceId,
                            messageEntry.addressFrom,
                            messageEntry.dateTime,
                            messageEntry.smsCenterDateTime,
                            messageEntry.body,
                            messageEntry.isSent,
                            messageEntry.id
                    )
            );
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

        @ColumnInfo(name = "sms_date_time")
        public String smsCenterDateTime;

        @ColumnInfo(name = "body")
        public String body;

        @ColumnInfo(name = "is_sent", index = true)
        public boolean isSent = false;

        @NonNull
        public static Message fromMessageContainer(@NonNull MessageContainer messageContainer) {
            Message message = new Message();

            message.deviceId = messageContainer.getDeviceId();
            message.addressFrom = messageContainer.getAddressFrom();
            message.dateTime = messageContainer.getDateTime();
            message.body = messageContainer.getBody();

            return message;
        }
    }
}
