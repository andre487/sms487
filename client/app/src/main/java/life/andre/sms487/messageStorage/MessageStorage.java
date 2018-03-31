package life.andre.sms487.messageStorage;

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

        @Insert
        void insert(Message message);
    }

    @SuppressWarnings("WeakerAccess")
    @Entity
    public static class Message {
        @PrimaryKey(autoGenerate = true)
        public int id;

        @ColumnInfo(name = "address_from")
        public String addressFrom;

        @ColumnInfo(name = "date_time", index = true)
        public String dateTime;

        @ColumnInfo(name = "body")
        public String body;
    }

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

    public void addMessages(ArrayList<MessageContainer> messages) {
        for (MessageContainer message : messages) {
            Message entry = new Message();

            entry.addressFrom = message.getAddressFrom();
            entry.dateTime = message.getDateTime();
            entry.body = message.getBody();

            messageDao.insert(entry);
        }
    }

    public List<MessageContainer> getMessagesTail() {
        List<Message> messageEntries = messageDao.getTail();

        ArrayList<MessageContainer> messages = new ArrayList<>();
        for (Message messageEntry : messageEntries) {
            messages.add(
                    new MessageContainer(
                            messageEntry.addressFrom,
                            messageEntry.dateTime,
                            messageEntry.body
                    )
            );
        }

        return messages;
    }
}
