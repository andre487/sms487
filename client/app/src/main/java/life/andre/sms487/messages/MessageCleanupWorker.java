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
    private static final String logTag = "MessageCleanupWorker";
    private static final String taskName = "MessageCleanupWorker";

    @NonNull
    private final MessageStorage messageStorage;

    public static void schedule() {
        PeriodicWorkRequest task = new PeriodicWorkRequest.Builder(
                MessageCleanupWorker.class,
                1, TimeUnit.DAYS
        ).setConstraints(
                new Constraints.Builder()
                        .setRequiresDeviceIdle(true)
                        .setRequiresCharging(true)
                        .build()
        ).build();

        WorkManager workManager = WorkManager.getInstance();
        workManager.enqueueUniquePeriodicWork(taskName, ExistingPeriodicWorkPolicy.KEEP, task);
    }

    public MessageCleanupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        messageStorage = new MessageStorage(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            int oldCount = messageStorage.deleteOldMessages();
            Logger.i(logTag, "Old messages deleted: " + oldCount);
            return Result.success();
        } catch (Exception e) {
            Logger.i(logTag, e.toString());
            e.printStackTrace();
            return Result.failure();
        }
    }
}
