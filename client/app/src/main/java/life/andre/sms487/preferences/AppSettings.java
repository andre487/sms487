package life.andre.sms487.preferences;

import java.util.concurrent.ExecutionException;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import life.andre.sms487.system.AppConstants;


public class AppSettings {
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
                sharedPreferences, SettingNames.SERVER_URL, serverUrl
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
        return sharedPreferences.getString(SettingNames.SERVER_URL, "");
    }

    public void saveServerKey(String serverKey) {
        SaveSettingsParams params = new SaveSettingsParams(
                sharedPreferences, SettingNames.SERVER_KEY, serverKey
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
        return sharedPreferences.getString(SettingNames.SERVER_KEY, "");
    }

    public boolean getNeedSendSms() {
        String val = sharedPreferences.getString(SettingNames.NEED_SEND_SMS, "");
        return val.equals("1");
    }

    public void saveNeedSendSms(boolean needSendSms) {
        SaveSettingsParams params = new SaveSettingsParams(
                sharedPreferences, SettingNames.NEED_SEND_SMS, needSendSms ? "1" : "0"
        );

        SaveSettingsAction action = new SaveSettingsAction();
        action.execute(params);

        try {
            showMessage(action.get());
        } catch (InterruptedException | ExecutionException e) {
            showMessage(e.toString());
        }
    }

    private void showMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
