package life.andre.sms487.settings;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Objects;

import life.andre.sms487.views.Toaster;


@SuppressWarnings("SameParameterValue")
public class AppSettings {
    public static final String SERVER_URL = "Server URL";
    public static final String SERVER_KEY = "Server key";
    public static final String NEED_SEND_SMS = "Send SMS to server";

    private static final int TYPE_STRING = 0;
    private static final int TYPE_BOOL = 1;

    private static AppSettings instance;

    @NonNull
    private final AppSettingStorage storage;

    public static void init(@NonNull Context ctx) {
        instance = new AppSettings(ctx);
    }

    @NonNull
    public static AppSettings getInstance() {
        return Objects.requireNonNull(instance, "Not initialized");
    }

    private AppSettings(@NonNull Context ctx) {
        storage = new AppSettingStorage(ctx);
    }

    @NonNull
    public String getServerUrl() {
        return getString(SERVER_URL);
    }

    @NonNull
    public String getServerKey() {
        return getString(SERVER_KEY);
    }

    public boolean getNeedSendSms() {
        return getBool(NEED_SEND_SMS);
    }

    public void saveServerUrl(@NonNull String serverUrl) {
        saveValue(SERVER_URL, serverUrl);
    }

    public void saveServerKey(@NonNull String serverKey) {
        saveValue(SERVER_KEY, serverKey);
    }

    public void saveNeedSendSms(boolean needSendSms) {
        saveValue(NEED_SEND_SMS, needSendSms);
    }

    @NonNull
    private String getString(@NonNull String name) {
        String val = getSettingsItem(name).strVal;
        return val == null ? "" : val;
    }

    private boolean getBool(@NonNull String name) {
        return getSettingsItem(name).boolVal;
    }


    private void saveValue(@NonNull String name, @NonNull String val) {
        String msg = saveSettingsItemToStorage(name, TYPE_STRING, val, false);
        Toaster.getInstance().show(msg);
    }

    private void saveValue(@NonNull String name, boolean val) {
        String msg = saveSettingsItemToStorage(name, TYPE_BOOL, "", val);
        Toaster.getInstance().show(msg);
    }

    @NonNull
    private AppSettingStorage.SettingsItem getSettingsItem(@NonNull String name) {
        return storage.get(name);
    }

    @NonNull
    private String saveSettingsItemToStorage(@NonNull String name, int type, @NonNull String strVal, boolean boolVal) {
        switch (type) {
            case TYPE_STRING:
                return saveStringToStorage(name, strVal);
            case TYPE_BOOL:
                return saveBoolToStorage(name, boolVal);
        }

        return "Error: unknown setting";
    }

    @NonNull
    private String saveStringToStorage(@NonNull String name, @NonNull String val) {
        storage.set(name, val.trim());
        return name + " saved";
    }

    @NonNull
    private String saveBoolToStorage(@NonNull String name, boolean val) {
        storage.set(name, val);
        return name + " is now " + val;
    }
}
