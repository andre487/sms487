package life.andre.sms487.messages;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

import life.andre.sms487.logging.Logger;

public class MessageCleanupWorker extends Worker {
    public static final String TAG = "MessageCleanupWorker";

    @NonNull
    private final MessageStorage messageStorage;

    public static void schedule() {
        PeriodicWorkRequest task = new PeriodicWorkRequest.Builder(
                MessageCleanupWorker.class,
                3, TimeUnit.HOURS
        ).build();

        WorkManager workManager = WorkManager.getInstance();
        workManager.enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.KEEP, task);
    }

    public MessageCleanupWorker(@NonNull Context ctx, @NonNull WorkerParameters workerParams) {
        super(ctx, workerParams);
        messageStorage = new MessageStorage(ctx);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            messageStorage.deleteOld();
            return Result.success();
        } catch (Exception e) {
            Logger.i(TAG, e.toString());
            e.printStackTrace();
            return Result.failure();
        }
    }
}
