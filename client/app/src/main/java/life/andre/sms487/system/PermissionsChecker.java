package life.andre.sms487.system;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
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
            ActivityCompat.requestPermissions(activity, permissions,
                    AppConstants.PERMISSIONS_REQUEST_ID);
        }

        boolean hasNotificationPermission = NotificationManagerCompat
                .getEnabledListenerPackages(activity)
                .contains(activity.getPackageName());

        if (!hasNotificationPermission) {
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            activity.startActivity(intent);
        }
    }
}
