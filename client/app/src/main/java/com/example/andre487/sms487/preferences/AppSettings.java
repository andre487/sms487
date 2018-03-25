package com.example.andre487.sms487.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.widget.Toast;

import com.example.andre487.sms487.logging.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutionException;


@SuppressWarnings("UnusedReturnValue")
public class AppSettings {
    private static final String PREFERENCES_KEY = "com.example.andre487.sms487.preferences";
    private static final String SERVER_URL = "server_url";
    private static final String USER_NAME = "user_name";
    private static final String SERVER_KEY = "server_key";

    private SharedPreferences sharedPreferences;

    private Context context;

    class SaveSettingParams {
        SharedPreferences sharedPreferences;
        Context context;
        String key;
        String value;

        SaveSettingParams(SharedPreferences sharedPreferences, Context context, String key,
                          String value) {
            this.sharedPreferences = sharedPreferences;
            this.context = context;
            this.key = key;
            this.value = value;
        }
    }

    static class SaveSettingAction extends AsyncTask<SaveSettingParams, Void, String> {
        @Override
        protected String doInBackground(SaveSettingParams... params) {
            if (params.length == 0) {
                Logger.w("SaveSettingAction", "Params length is 0");
                return null;
            }

            SaveSettingParams mainParams = params[0];

            switch (mainParams.key) {
                case SERVER_URL:
                    return saveServerUrl(mainParams);
                case USER_NAME:
                    return saveUserName(mainParams);
                case SERVER_KEY:
                    return saveServerKey(mainParams);
            }

            return null;
        }

        private String saveServerUrl(SaveSettingParams params) {
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

        private String saveUserName(SaveSettingParams params) {
            String userName = params.value.trim();
            if (userName.length() == 0) {
                return "Error: user name is empty";
            }

            params.sharedPreferences.edit().putString(USER_NAME, userName).apply();

            return "User name saved";
        }

        private String saveServerKey(SaveSettingParams params) {
            String serverKey = params.value.trim();
            if (serverKey.length() == 0) {
                return "Error: server key is empty";
            }

            params.sharedPreferences.edit().putString(SERVER_KEY, serverKey).apply();

            return "Server key saved";
        }
    }

    public AppSettings(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(
                PREFERENCES_KEY,
                Context.MODE_PRIVATE
        );
    }

    public void saveServerUrl(String serverUrl) {
        SaveSettingParams params = new SaveSettingParams(
                sharedPreferences, context,
                SERVER_URL, serverUrl
        );

        SaveSettingAction action = new SaveSettingAction();
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

    public void saveUserName(String userName) {
        SaveSettingParams params = new SaveSettingParams(
                sharedPreferences, context,
                USER_NAME, userName
        );

        SaveSettingAction action = new SaveSettingAction();
        action.execute(params);

        try {
            showMessage(action.get());
        } catch (InterruptedException | ExecutionException e) {
            showMessage(e.toString());
        }
    }

    public String getUserName() {
        return sharedPreferences.getString(USER_NAME, "");
    }

    public void saveServerKey(String serverKey) {
        SaveSettingParams params = new SaveSettingParams(
                sharedPreferences, context,
                SERVER_KEY, serverKey
        );

        SaveSettingAction action = new SaveSettingAction();
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

    private void showMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
