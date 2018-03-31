package life.andre.sms487.preferences;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import life.andre.sms487.logging.Logger;

class SaveSettingsAction extends AsyncTask<SaveSettingsParams, Void, String> {
    @Override
    protected String doInBackground(SaveSettingsParams... params) {
        if (params.length == 0) {
            Logger.w("SaveSettingAction", "Params length is 0");
            return null;
        }

        SaveSettingsParams mainParams = params[0];

        switch (mainParams.key) {
            case SettingNames.SERVER_URL:
                return saveServerUrl(mainParams);
            case SettingNames.USER_NAME:
                return saveUserName(mainParams);
            case SettingNames.SERVER_KEY:
                return saveServerKey(mainParams);
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

        params.sharedPreferences.edit().putString(SettingNames.SERVER_URL, params.value).apply();

        return "Server URL saved";
    }

    private String saveUserName(SaveSettingsParams params) {
        String userName = params.value.trim();
        if (userName.length() == 0) {
            return "Error: user name is empty";
        }

        params.sharedPreferences.edit().putString(SettingNames.USER_NAME, userName).apply();

        return "User name saved";
    }

    private String saveServerKey(SaveSettingsParams params) {
        String serverKey = params.value.trim();
        if (serverKey.length() == 0) {
            return "Error: server key is empty";
        }

        params.sharedPreferences.edit().putString(SettingNames.SERVER_KEY, serverKey).apply();

        return "Server key saved";
    }
}
