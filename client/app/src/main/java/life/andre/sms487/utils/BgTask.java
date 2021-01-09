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

    private static Executor executor;
    @Nullable
    private Handler handler;

    @NonNull
    private final List<Consumer<Result>> successCallbacks = new ArrayList<>();
    @NonNull
    private final List<Consumer<Exception>> errorCallbacks = new ArrayList<>();

    @NonNull
    private final Callable<Result> task;
    private int state = STATE_INITIAL;
    private final boolean createdOnUiThread;

    @Nullable
    private Result result;
    @Nullable
    private Exception error;

    @NonNull
    public static <Result> BgTask<Result> run(@NonNull Callable<Result> task) {
        return new BgTask<>(task).run();
    }

    private static synchronized void initExecutor() {
        if (executor != null) {
            return;
        }
        int cores = Runtime.getRuntime().availableProcessors();
        int poolSize = Math.max(cores - 2, 2);
        executor = Executors.newFixedThreadPool(poolSize);
    }

    BgTask(@NonNull Callable<Result> task) {
        initExecutor();
        this.task = task;

        createdOnUiThread = Looper.getMainLooper().isCurrentThread();
        if (createdOnUiThread) {
            handler = new Handler(Looper.getMainLooper());
        }
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
                Result res = task.call();
                synchronized (this) {
                    result = res;
                    handleSuccess();
                }
            } catch (@NonNull final Exception err) {
                synchronized (this) {
                    error = err;
                    handleError();
                }
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
            if (handler == null) {
                callback.accept(result);
            } else {
                handler.post(() -> callback.accept(result));
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
            if (handler == null) {
                callback.accept(error);
            } else {
                handler.post(() -> callback.accept(error));
            }
            return this;
        }

        errorCallbacks.add(callback);
        return this;
    }

    private void handleSuccess() {
        state = STATE_SUCCESS;

        if (successCallbacks.size() == 0) {
            clearCallbacks();
            return;
        }

        if (!createdOnUiThread || handler == null) {
            callSuccessCallbacks();
            return;
        }

        handler.post(() -> {
            synchronized (this) {
                callSuccessCallbacks();
            }
        });
    }

    private void handleError() {
        state = STATE_ERROR;

        if (error == null) {
            error = new Exception("Unknown error");
        }

        if (errorCallbacks.size() == 0) {
            logErrorReport();
            clearCallbacks();
            return;
        }

        if (!createdOnUiThread || handler == null) {
            callErrorCallbacks();
            return;
        }

        handler.post(() -> {
            synchronized (this) {
                callErrorCallbacks();
            }
        });
    }

    private void callSuccessCallbacks() {
        for (Consumer<Result> callback : successCallbacks) {
            callback.accept(result);
        }
        clearCallbacks();
    }

    private void callErrorCallbacks() {
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
        report.append(error.toString());
        report.append("\n");

        for (StackTraceElement trace : error.getStackTrace()) {
            if (trace == null) {
                continue;
            }
            report.append("\tat ");
            report.append(trace.toString());
            report.append("\n");
        }

        Logger.e(TAG, report.toString());
    }

    private void clearCallbacks() {
        successCallbacks.clear();
        errorCallbacks.clear();
    }
}
