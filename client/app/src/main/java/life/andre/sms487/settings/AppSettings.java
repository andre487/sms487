package life.andre.sms487.settings;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutionException;

import life.andre.sms487.utils.AsyncTaskUtil;


@SuppressWarnings("SameParameterValue")
public class AppSettings {
    public static final String TAG = "AppSettings";
    public static final String SERVER_URL = "Server URL";
    public static final String SERVER_KEY = "Server key";
    public static final String NEED_SEND_SMS = "Send SMS to server";

    @NonNull
    private final Context ctx;

    @NonNull
    private final AppSettingStorage storage;

    public AppSettings(@NonNull Context ctx) {
        this.ctx = ctx;
        storage = new AppSettingStorage(ctx);
    }

    @NonNull
    public String getServerUrl() {
        return getString(SERVER_URL);
    }

    public void saveServerUrl(@NonNull String serverUrl) {
        saveValue(SERVER_URL, serverUrl);
    }

    @NonNull
    public String getServerKey() {
        return getString(SERVER_KEY);
    }

    public void saveServerKey(@NonNull String serverKey) {
        saveValue(SERVER_KEY, serverKey);
    }

    public boolean getNeedSendSms() {
        return getBool(NEED_SEND_SMS);
    }

    public void saveNeedSendSms(boolean needSendSms) {
        saveValue(NEED_SEND_SMS, needSendSms);
    }

    @NonNull
    private String getString(@NonNull String name) {
        if (!Looper.getMainLooper().isCurrentThread()) {
            String val = getSettingsItem(name).strVal;
            return val == null ? "" : val;
        }

        GetSettingsAction action = new GetSettingsAction();
        action.execute(new GetSettingsParams(this, name));

        String val = null;
        try {
            val = action.get().strVal;
        } catch (@NonNull ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return val == null ? "" : val;
    }

    private boolean getBool(@NonNull String name) {
        if (!Looper.getMainLooper().isCurrentThread()) {
            return getSettingsItem(name).boolVal;
        }

        GetSettingsAction action = new GetSettingsAction();
        action.execute(new GetSettingsParams(this, name));

        boolean val = false;
        try {
            val = action.get().boolVal;
        } catch (@NonNull ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return val;
    }


    private void saveValue(@NonNull String name, @NonNull String val) {
        String message = _saveValue(new SaveSettingsParams(this, name, val));
        showMessage(message);
    }

    private void saveValue(@NonNull String name, boolean val) {
        String message = _saveValue(new SaveSettingsParams(this, name, val));
        showMessage(message);
    }

    @NonNull
    private String _saveValue(SaveSettingsParams params) {
        if (!Looper.getMainLooper().isCurrentThread()) {
            return saveSettingsItemToStorage(params);
        }

        SaveSettingsAction action = new SaveSettingsAction();
        action.execute(params);

        try {
            return action.get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    @NonNull
    private AppSettingStorage.SettingsItem getSettingsItem(@NonNull String name) {
        return storage.get(name);
    }

    private String saveSettingsItemToStorage(SaveSettingsParams params) {
        if (params.name.equals(SERVER_URL)) {
            return saveServerUrlToStorage(params);
        }

        switch (params.type) {
            case SaveSettingsParams.TYPE_STRING:
                return saveStringToStorage(params);
            case SaveSettingsParams.TYPE_BOOL:
                return saveBoolToStorage(params);
        }

        return "Error: unknown setting";
    }

    @NonNull
    private String saveServerUrlToStorage(@NonNull SaveSettingsParams params) {
        try {
            URL url = new URL(params.strVal);
            URLConnection conn = url.openConnection();
            conn.connect();
        } catch (MalformedURLException e) {
            return "Incorrect URL format";
        } catch (IOException e) {
            return "Could not connect to URL";
        }

        storage.set(SERVER_URL, params.strVal);

        return "Server URL saved";
    }

    @NonNull
    private String saveStringToStorage(@NonNull SaveSettingsParams params) {
        String val = params.strVal.trim();
        if (val.length() == 0) {
            return "Error: value is empty";
        }

        storage.set(params.name, val);

        return "Server key saved";
    }

    @NonNull
    private String saveBoolToStorage(@NonNull SaveSettingsParams params) {
        storage.set(params.name, params.boolVal);

        return params.name + " is now " + params.boolVal;
    }

    private void showMessage(String message) {
        if (Looper.getMainLooper().isCurrentThread()) {
            Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
        }
    }

    static class GetSettingsParams {
        AppSettings settings;
        String name;

        GetSettingsParams(AppSettings settings, String name) {
            this.settings = settings;
            this.name = name;
        }
    }

    static class GetSettingsAction extends AsyncTask<GetSettingsParams, Void, AppSettingStorage.SettingsItem> {
        @Override
        @NonNull
        protected AppSettingStorage.SettingsItem doInBackground(GetSettingsParams... params) {
            GetSettingsParams mainParams = AsyncTaskUtil.getParams(params, TAG);
            if (mainParams == null) {
                return new AppSettingStorage.SettingsItem();
            }
            return mainParams.settings.getSettingsItem(mainParams.name);
        }
    }

    static class SaveSettingsParams {
        static final int TYPE_STRING = 1;
        static final int TYPE_BOOL = 2;

        int type;

        @NonNull
        final AppSettings settings;
        @NonNull
        final String name;

        @NonNull
        String strVal = "";
        boolean boolVal = false;

        SaveSettingsParams(@NonNull AppSettings settings, @NonNull String name, @NonNull String val) {
            this.settings = settings;
            this.name = name;
            this.type = TYPE_STRING;
            this.strVal = val;
        }

        SaveSettingsParams(@NonNull AppSettings settings, @NonNull String name, boolean val) {
            this.settings = settings;
            this.name = name;
            this.type = TYPE_BOOL;
            this.boolVal = val;
        }
    }

    static class SaveSettingsAction extends AsyncTask<SaveSettingsParams, Void, String> {
        @Override
        @NonNull
        protected String doInBackground(@NonNull SaveSettingsParams... params) {
            SaveSettingsParams mainParams = AsyncTaskUtil.getParams(params, TAG);
            if (mainParams == null) {
                return "Error: empty params";
            }
            return mainParams.settings.saveSettingsItemToStorage(mainParams);
        }
    }
}
