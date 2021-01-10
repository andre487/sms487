package life.andre.sms487.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import life.andre.sms487.logging.Logger;

public class BgTask<Result> {
    public static final String TAG = "BgTask";

    public static final int STATE_INITIAL = 0;
    public static final int STATE_RUNNING = 1;
    public static final int STATE_SUCCESS = 2;
    public static final int STATE_ERROR = 3;

    @NonNull
    private static final Executor executor;

    @NonNull
    private static final Handler uiHandler = new Handler(Looper.getMainLooper());

    @NonNull
    private final List<Consumer<Result>> successCallbacks = new ArrayList<>();
    @NonNull
    private final List<Consumer<Exception>> errorCallbacks = new ArrayList<>();

    @NonNull
    private final Callable<Result> task;
    private volatile int state = STATE_INITIAL;
    private final boolean createdOnUiThread;

    @Nullable
    private Result result;
    @Nullable
    private Exception error;

    static {
        int cores = Runtime.getRuntime().availableProcessors();
        int poolSize = Math.max(cores - 2, 2);
        executor = Executors.newFixedThreadPool(poolSize);
    }

    @NonNull
    public static <Result> BgTask<Result> run(@NonNull Callable<Result> task) {
        return new BgTask<>(task).run();
    }

    BgTask(@NonNull Callable<Result> task) {
        this.task = task;
        createdOnUiThread = Looper.getMainLooper().isCurrentThread();
    }

    @NonNull
    public synchronized BgTask<Result> run() {
        if (state > STATE_INITIAL) {
            return this;
        }

        state = STATE_RUNNING;
        result = null;
        error = null;
        clearCallbacks();

        executor.execute(() -> {
            try {
                result = task.call();
                handleSuccess();
            } catch (@NonNull final Exception err) {
                error = err;
                handleError();
            }
        });

        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    public synchronized BgTask<Result> onSuccess(@NonNull Consumer<Result> callback) {
        if (state == STATE_ERROR) {
            return this;
        }

        if (state == STATE_SUCCESS) {
            if (createdOnUiThread) {
                uiHandler.post(() -> callback.accept(result));
            } else {
                callback.accept(result);
            }
            return this;
        }

        successCallbacks.add(callback);
        return this;
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    @NonNull
    public synchronized BgTask<Result> onError(@NonNull Consumer<Exception> callback) {
        if (state == STATE_SUCCESS) {
            return this;
        }

        if (state == STATE_ERROR) {
            if (createdOnUiThread) {
                uiHandler.post(() -> callback.accept(error));
            } else {
                callback.accept(error);
            }
            return this;
        }

        errorCallbacks.add(callback);
        return this;
    }

    private synchronized void handleSuccess() {
        state = STATE_SUCCESS;

        if (successCallbacks.size() == 0) {
            clearCallbacks();
            return;
        }

        if (!createdOnUiThread) {
            callSuccessCallbacks();
            return;
        }

        uiHandler.post(this::callSuccessCallbacks);
    }

    private synchronized void handleError() {
        state = STATE_ERROR;

        if (error == null) {
            error = new Exception("Unknown error");
        }

        if (errorCallbacks.size() == 0) {
            logErrorReport();
            clearCallbacks();
            return;
        }

        if (!createdOnUiThread) {
            callErrorCallbacks();
            return;
        }

        uiHandler.post(this::callErrorCallbacks);
    }

    private synchronized void callSuccessCallbacks() {
        for (Consumer<Result> callback : successCallbacks) {
            callback.accept(result);
        }
        clearCallbacks();
    }

    private synchronized void callErrorCallbacks() {
        for (Consumer<Exception> callback : errorCallbacks) {
            callback.accept(error);
        }
        clearCallbacks();
    }

    private void logErrorReport() {
        if (error == null) {
            Logger.e(TAG, "There was an error and it is null");
            return;
        }

        StringBuilder report = new StringBuilder();
        report.append(error.toString()).append("\n");

        for (StackTraceElement trace : error.getStackTrace()) {
            if (trace != null) {
                report.append("\tat ").append(trace.toString()).append("\n");
            }
        }

        Logger.e(TAG, report.toString());
    }

    private void clearCallbacks() {
        successCallbacks.clear();
        errorCallbacks.clear();
    }
}
