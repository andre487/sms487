package life.andre.sms487.system;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutionException;

import life.andre.sms487.utils.AsyncTaskUtil;


public class AppSettings {
    private static final String SERVER_URL = "server_url";
    private static final String SERVER_KEY = "server_key";
    private static final String NEED_SEND_SMS = "need_send_sms";
    private static final String logTag = "AppSettings";

    private final SharedPreferences sharedPreferences;
    private final Context context;

    public AppSettings(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(
                AppConstants.PREFERENCES_KEY,
                Context.MODE_PRIVATE
        );
    }

    public void saveServerUrl(String serverUrl) {
        SaveSettingsParams params = new SaveSettingsParams(
                sharedPreferences, SERVER_URL, serverUrl
        );

        SaveSettingsAction action = new SaveSettingsAction();
        action.execute(params);

        try {
            showMessage(action.get());
        } catch (InterruptedException | ExecutionException e) {
            showMessage(e.toString());
        }
    }

    public String getServerUrl() {
        return sharedPreferences.getString(SERVER_URL, "");
    }

    public void saveServerKey(String serverKey) {
        SaveSettingsParams params = new SaveSettingsParams(
                sharedPreferences, SERVER_KEY, serverKey
        );

        SaveSettingsAction action = new SaveSettingsAction();
        action.execute(params);

        try {
            showMessage(action.get());
        } catch (InterruptedException | ExecutionException e) {
            showMessage(e.toString());
        }
    }

    public String getServerKey() {
        return sharedPreferences.getString(SERVER_KEY, "");
    }

    public boolean getNeedSendSms() {
        String val = sharedPreferences.getString(NEED_SEND_SMS, "");
        return val.equals("1");
    }

    public void saveNeedSendSms(boolean needSendSms) {
        SaveSettingsParams params = new SaveSettingsParams(
                sharedPreferences, NEED_SEND_SMS, needSendSms ? "1" : "0"
        );

        SaveSettingsAction action = new SaveSettingsAction();
        action.execute(params);

        try {
            String val = action.get();
            if (val == null) {
                val = "Param value is null";
            }
            showMessage(val);
        } catch (InterruptedException | ExecutionException e) {
            showMessage(e.toString());
        }
    }

    private void showMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    static class SaveSettingsParams {
        SharedPreferences sharedPreferences;
        String key;
        String value;

        SaveSettingsParams(SharedPreferences sharedPreferences, String key, String value) {
            this.sharedPreferences = sharedPreferences;
            this.key = key;
            this.value = value;
        }
    }

    static class SaveSettingsAction extends AsyncTask<SaveSettingsParams, Void, String> {
        @Override
        @Nullable
        protected String doInBackground(SaveSettingsParams... params) {
            SaveSettingsParams mainParams = AsyncTaskUtil.getParams(params, logTag);
            if (mainParams == null) {
                return null;
            }

            switch (mainParams.key) {
                case SERVER_URL:
                    return saveServerUrl(mainParams);
                case SERVER_KEY:
                    return saveServerKey(mainParams);
                case NEED_SEND_SMS:
                    return saveNeedSendSms(mainParams);
            }

            return null;
        }

        private String saveServerUrl(SaveSettingsParams params) {
            try {
                URL url = new URL(params.value);
                URLConnection conn = url.openConnection();
                conn.connect();
            } catch (MalformedURLException e) {
                return "Incorrect URL format";
            } catch (IOException e) {
                return "Could not connect to URL";
            }

            params.sharedPreferences.edit().putString(SERVER_URL, params.value).apply();

            return "Server URL saved";
        }

        private String saveServerKey(SaveSettingsParams params) {
            String serverKey = params.value.trim();
            if (serverKey.length() == 0) {
                return "Error: server key is empty";
            }

            params.sharedPreferences.edit().putString(SERVER_KEY, serverKey).apply();

            return "Server key saved";
        }

        private String saveNeedSendSms(SaveSettingsParams params) {
            String val = params.value.trim();
            if (!val.equals("1") && !val.equals("0")) {
                return "Error: invalid send SMS param";
            }

            params.sharedPreferences.edit().putString(NEED_SEND_SMS, val).apply();

            return val.equals("1") ?
                    "SMS will be sent to the server now" :
                    "SMS will not be sent to the server now";
        }
    }
}