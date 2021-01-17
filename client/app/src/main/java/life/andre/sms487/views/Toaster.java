package life.andre.sms487.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Objects;

import life.andre.sms487.utils.StringUtil;
import life.andre.sms487.utils.ValueThrottler;

public class Toaster {
    public static final int MESSAGE_DELAY = 250;

    @SuppressLint("StaticFieldLeak")
    private static Toaster instance;

    @NonNull
    private final ValueThrottler<String> throttler = new ValueThrottler<>(this::handleThrottled, MESSAGE_DELAY);
    @NonNull
    private final Handler handler = new Handler(Looper.getMainLooper());
    @NonNull
    private final Context ctx;

    public static void init(@NonNull Context ctx) {
        instance = new Toaster(ctx);
    }

    @NonNull
    public static Toaster getInstance() {
        return Objects.requireNonNull(instance, "Not initialized");
    }

    private Toaster(@NonNull Context ctx) {
        this.ctx = ctx;
    }

    public void show(@Nullable String msg) {
        if (msg == null) {
            return;
        }
        handler.post(() -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show());
    }

    public void showThrottled(@Nullable String msg) {
        if (msg == null) {
            return;
        }
        throttler.handle(msg);
    }

    private void handleThrottled(@NonNull List<String> messages) {
        String msg = StringUtil.joinUnique(messages, "\n");
        show(msg);
    }
}
