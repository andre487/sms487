package life.andre.sms487.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import life.andre.sms487.utils.StringUtil;
import life.andre.sms487.utils.ValueThrottler;

public class Toaster {
    public static final int MESSAGE_DELAY = 250;

    @SuppressLint("StaticFieldLeak")
    private static Context ctx;
    @NonNull
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private final static ValueThrottler<String> throttler = new ValueThrottler<>(Toaster::handleThrottled, MESSAGE_DELAY);

    public static void init(Context ctx) {
        Toaster.ctx = ctx;
    }

    public static void show(@Nullable String msg) {
        if (msg == null) {
            return;
        }
        handler.post(() -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show());
    }

    public static void showThrottled(@Nullable String msg) {
        if (msg == null) {
            return;
        }
        throttler.handle(msg);
    }

    private static void handleThrottled(@NonNull List<String> messages) {
        String msg = StringUtil.joinUnique(messages, "\n");
        show(msg);
    }
}
