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
    public static final String TAG = "MessageResendWorker";

    @NonNull
    private final ServerApi serverApi;

    public static void scheduleOneTime() {
        OneTimeWorkRequest task = new OneTimeWorkRequest.Builder(
                MessageResendWorker.class
        ).setConstraints(
                new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
        ).build();

        WorkManager workManager = WorkManager.getInstance();
        workManager.enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, task);

        Logger.i(TAG, "Schedule messages resend");
    }

    public MessageResendWorker(@NonNull Context ctx, @NonNull WorkerParameters workerParams) {
        super(ctx, workerParams);
        serverApi = ServerApi.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Logger.i(TAG, "Resend messages if needed");
            serverApi.resendMessages();
            return Result.success();
        } catch (Exception e) {
            Logger.i(TAG, e.toString());
            e.printStackTrace();
            return Result.failure();
        }
    }
}
