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
            case SettingNames.SERVER_KEY:
                return saveServerKey(mainParams);
            case SettingNames.NEED_SEND_SMS:
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

        params.sharedPreferences.edit().putString(SettingNames.SERVER_URL, params.value).apply();

        return "Server URL saved";
    }

    private String saveServerKey(SaveSettingsParams params) {
        String serverKey = params.value.trim();
        if (serverKey.length() == 0) {
            return "Error: server key is empty";
        }

        params.sharedPreferences.edit().putString(SettingNames.SERVER_KEY, serverKey).apply();

        return "Server key saved";
    }

    private String saveNeedSendSms(SaveSettingsParams params) {
        String val = params.value.trim();
        if (!val.equals("1") && !val.equals("0")) {
            return "Error: invalid send SMS param";
        }

        params.sharedPreferences.edit().putString(SettingNames.NEED_SEND_SMS, val).apply();

        return val.equals("1") ?
                "SMS will be sent to the server now" :
                "SMS will not be sent to the server now";
    }
}
