package life.andre.sms487.system;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionsChecker {
    private final Activity activity;

    public PermissionsChecker(Activity activity) {
        this.activity = activity;
    }

    public void checkPermissions() {
        List<String> requiredPermissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.RECEIVE_SMS);
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.INTERNET);
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECEIVE_BOOT_COMPLETED)
                != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.RECEIVE_BOOT_COMPLETED);
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.FOREGROUND_SERVICE)
                != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.FOREGROUND_SERVICE);
        }

        int permCount = requiredPermissions.size();
        if (permCount > 0) {
            String[] permissions = requiredPermissions.toArray(new String[permCount]);
            activity.requestPermissions(permissions, AppConstants.DEFAULT_ID);
        }

        boolean hasNotificationPermission = Settings.Secure.getString(
                activity.getContentResolver(),
                "enabled_notification_listeners"
        ).contains(activity.getPackageName());

        if (!hasNotificationPermission) {
            Intent intent = new Intent(AppConstants.NOTIFICATION_SETTING_ACTIVITY);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(intent);
        }
    }
}
