package life.andre.sms487.system;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class PermissionsChecker {
    public static final String NOTIFICATION_SETTINGS_ACTIVITY = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    public static final int REQUEST_ID = 0;

    public static void check(@NonNull Activity activity) {
        String[] required = {
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.RECEIVE_BOOT_COMPLETED,
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.POST_NOTIFICATIONS,
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
            activity.requestPermissions(permissions, REQUEST_ID);
        }

        String notificationListeners = Settings.Secure.getString(
                activity.getContentResolver(),
                "enabled_notification_listeners"
        );

        if (!notificationListeners.contains(activity.getPackageName())) {
            Intent intent = new Intent(NOTIFICATION_SETTINGS_ACTIVITY).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(intent);
        }
    }
}
