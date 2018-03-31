package life.andre.sms487.system;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionsChecker {
    private Activity activity;

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

        int permCount = requiredPermissions.size();
        if (permCount > 0) {
            String[] permissions = requiredPermissions.toArray(new String[permCount]);
            ActivityCompat.requestPermissions(activity, permissions,
                    AppConstants.PERMISSIONS_REQUEST_ID);
        }
    }
}