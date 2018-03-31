package life.andre.sms487.preferences;

import android.content.SharedPreferences;

class SaveSettingsParams {
    SharedPreferences sharedPreferences;
    String key;
    String value;

    SaveSettingsParams(SharedPreferences sharedPreferences, String key, String value) {
        this.sharedPreferences = sharedPreferences;
        this.key = key;
        this.value = value;
    }
}
