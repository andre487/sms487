package life.andre.sms487.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ValueThrottler<ValType> {
    @NonNull
    public static final String TAG = "ValueThrottler";

    @NonNull
    private final List<ValType> values = new ArrayList<>();
    @NonNull
    private final Consumer<List<ValType>> callback;
    private final long delayMillis;
    @NonNull
    private final Runnable handlerCallback = this::runCallback;

    @NonNull
    private final Handler delayHandler = new Handler(Looper.getMainLooper());

    public ValueThrottler(@NonNull Consumer<List<ValType>> callback, long delayMillis) {
        this.callback = callback;
        this.delayMillis = delayMillis;
    }

    public synchronized void handle(@NonNull ValType value) {
        values.add(value);
        if (!delayHandler.hasCallbacks(handlerCallback)) {
            delayHandler.postDelayed(handlerCallback, delayMillis);
        }
    }

    private synchronized void runCallback() {
        callback.accept(new ArrayList<>(values));
        values.clear();
        delayHandler.removeCallbacks(handlerCallback);
    }
}
