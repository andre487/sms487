package life.andre.sms487.messages;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.network.ServerApi;

public class MessageResendWorker extends Worker {
    public static final String TAG = "MRW";
    public static final String TASK_ID = "MessageResendWorker";

    public static void scheduleOneTime(@NonNull Context ctx) {
        OneTimeWorkRequest task = new OneTimeWorkRequest.Builder(
            MessageResendWorker.class
        ).setConstraints(
            new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build();

        WorkManager workManager = WorkManager.getInstance(ctx);
        workManager.enqueueUniqueWork(TASK_ID, ExistingWorkPolicy.KEEP, task);

        Logger.i(TAG, "Schedule messages resend");
    }

    public MessageResendWorker(@NonNull Context ctx, @NonNull WorkerParameters workerParams) {
        super(ctx, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            ServerApi.getInstance().resendMessages();
            return Result.success();
        } catch (Exception e) {
            Logger.e(TAG, e.toString());
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            return Result.failure();
        }
    }
}
