package life.andre.sms487.preferences;

import java.util.concurrent.ExecutionException;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import life.andre.sms487.system.AppConstants;


public class AppSettings {
    private SharedPreferences sharedPreferences;

    private Context context;

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

    public void saveUserName(String userName) {
        SaveSettingsParams params = new SaveSettingsParams(
                sharedPreferences, SettingNames.USER_NAME, userName
        );

        SaveSettingsAction action = new SaveSettingsAction();
        action.execute(params);

        try {
            showMessage(action.get());
        } catch (InterruptedException | ExecutionException e) {
            showMessage(e.toString());
        }
    }

    public String getUserName() {
        return sharedPreferences.getString(SettingNames.USER_NAME, "");
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

    private void showMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}