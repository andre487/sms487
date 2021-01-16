package life.andre.sms487.system;

import android.app.Application;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.views.Toaster;

public class ApplicationEntryPoint extends Application {
    public static final String TAG = "ApplicationEntryPoint";

    @Override
    public void onCreate() {
        super.onCreate();
        Toaster.init(getApplicationContext());
        Logger.d(TAG, "Application started");
    }
}
