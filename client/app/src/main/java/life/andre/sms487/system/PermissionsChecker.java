package life.andre.sms487.system;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

public class PermissionsChecker {
    private final Activity activity;

    public PermissionsChecker(Activity activity) {
        this.activity = activity;
    }

    public void checkPermissions() {
        String[] required = {
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.RECEIVE_BOOT_COMPLETED,
                Manifest.permission.FOREGROUND_SERVICE,
        };

        List<String> absent = new ArrayList<>();

        for (String permission : required) {
            if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                absent.add(permission);
            }
        }

        int count = absent.size();
        if (count > 0) {
            String[] permissions = absent.toArray(new String[count]);
            activity.requestPermissions(permissions, AppConstants.DEFAULT_ID);
        }

        String notificationListeners = Settings.Secure.getString(
                activity.getContentResolver(),
                "enabled_notification_listeners"
        );

        if (!notificationListeners.contains(activity.getPackageName())) {
            activity.startActivity(new Intent(AppConstants.NOTIFICATION_SETTINGS_ACTIVITY));
        }
    }
}
