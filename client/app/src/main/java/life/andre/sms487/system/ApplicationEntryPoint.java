package life.andre.sms487.system;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageCleanupWorker;
import life.andre.sms487.messages.MessageResendWorker;
import life.andre.sms487.messages.MessageStorage;
import life.andre.sms487.network.ServerApi;
import life.andre.sms487.services.NotificationListener;
import life.andre.sms487.settings.AppSettings;
import life.andre.sms487.views.Toaster;

public class ApplicationEntryPoint extends Application {
    public static final String TAG = "APP";

    @Override
    public void onCreate() {
        super.onCreate();
        initGlobalServiceObjects();
        startServiceTasks();
        Logger.i(TAG, "Application started");
    }

    private void initGlobalServiceObjects() {
        Context ctx = getApplicationContext();

        AppSettings.init(ctx);
        MessageStorage.init(ctx);
        ServerApi.init(ctx);
        Toaster.init(ctx);
    }

    private void startServiceTasks() {
        Intent intent = new Intent(this, NotificationListener.class);
        startService(intent);

        MessageCleanupWorker.schedulePeriodic();
        MessageResendWorker.scheduleOneTime();
    }
}
