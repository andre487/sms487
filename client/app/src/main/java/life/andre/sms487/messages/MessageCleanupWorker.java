package life.andre.sms487.messages;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

import life.andre.sms487.logging.Logger;

public class MessageCleanupWorker extends Worker {
    public static final String TAG = "MCW";
    public static final String TASK_ID = "MessageCleanupWorker";

    public static void schedulePeriodic(@NonNull Context ctx) {
        PeriodicWorkRequest task = new PeriodicWorkRequest.Builder(
                MessageCleanupWorker.class,
                1, TimeUnit.DAYS
        ).setConstraints(
                new Constraints.Builder()
                        .setRequiresCharging(true)
                        .setRequiresDeviceIdle(true)
                        .build()
        ).build();

        WorkManager workManager = WorkManager.getInstance(ctx);
        workManager.enqueueUniquePeriodicWork(TASK_ID, ExistingPeriodicWorkPolicy.KEEP, task);

        Logger.i(TAG, "Schedule old messages cleanup");
    }

    public MessageCleanupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        MessageStorage messageStorage = MessageStorage.getInstance();
        try {
            messageStorage.deleteOld();
            return Result.success();
        } catch (Exception e) {
            Logger.e(TAG, e.toString());
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            return Result.failure();
        }
    }
}
