package life.andre.sms487.messages;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.network.SmsApi;
import life.andre.sms487.system.AppSettings;

public class MessageResendWorker extends Worker {
    private static final String logTag = "MessageResendWorker";
    private static final String taskName = "MessageResendWorker";

    @NonNull
    private final SmsApi smsApi;

    public static void schedule() {
        PeriodicWorkRequest task = new PeriodicWorkRequest.Builder(
                MessageResendWorker.class,
                15, TimeUnit.MINUTES
        ).setConstraints(
                new Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
        ).setBackoffCriteria(
                BackoffPolicy.LINEAR,
                PeriodicWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
        ).build();

        WorkManager workManager = WorkManager.getInstance();
        workManager.enqueueUniquePeriodicWork(taskName, ExistingPeriodicWorkPolicy.KEEP, task);
    }

    public MessageResendWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        smsApi = new SmsApi(context, new AppSettings(context));
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            smsApi.resendMessages();
            Logger.i(logTag, "Try to resend messages");
            return Result.success();
        } catch (Exception e) {
            Logger.i(logTag, e.toString());
            e.printStackTrace();
            return Result.failure();
        }
    }
}
