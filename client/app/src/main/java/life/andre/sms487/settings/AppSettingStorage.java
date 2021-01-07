package life.andre.sms487.settings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;

public class AppSettingStorage {
    private final SettingsDao dao;

    AppSettingStorage(@NonNull Context ctx) {
        dao = Room.databaseBuilder(ctx, SettingsDatabase.class, "settings")
                .build()
                .getSettingsDao();
    }

    public void set(@NonNull String name, @NonNull String val) {
        SettingsItem item = new SettingsItem();
        item.name = name;
        item.strVal = val;

        dao.set(item);
    }

    public void set(@NonNull String name, boolean val) {
        SettingsItem item = new SettingsItem();
        item.name = name;
        item.boolVal = val;

        dao.set(item);
    }

    @NonNull
    public SettingsItem get(@NonNull String name) {
        SettingsItem item = dao.get(name);
        if (item == null) {
            item = new SettingsItem();
        }
        return item;
    }

    @SuppressWarnings("NotNullFieldNotInitialized")
    @Entity
    public static class SettingsItem {
        @NonNull
        @PrimaryKey
        public String name;

        @Nullable
        @ColumnInfo(defaultValue = "")
        public String strVal;

        @ColumnInfo(defaultValue = "false")
        public boolean boolVal;

        @ColumnInfo(defaultValue = "0")
        public int intVal;

        @ColumnInfo(defaultValue = "0")
        public double doubleVal;
    }

    @Dao
    public interface SettingsDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void set(SettingsItem item);

        @Query("SELECT * FROM SettingsItem WHERE name=:name LIMIT 1")
        SettingsItem get(String name);
    }

    @Database(entities = {SettingsItem.class}, version = 1, exportSchema = false)
    public static abstract class SettingsDatabase extends RoomDatabase {
        public abstract SettingsDao getSettingsDao();
    }
}
