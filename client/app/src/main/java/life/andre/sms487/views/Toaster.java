package life.andre.sms487.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Toaster {
    @SuppressLint("StaticFieldLeak")
    private static Context ctx;

    @NonNull
    private static final Handler handler = new Handler(Looper.getMainLooper());

    public static void init(Context ctx) {
        Toaster.ctx = ctx;
    }

    public static void showMessage(@Nullable String msg) {
        if (msg == null) {
            return;
        }
        handler.post(() -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show());
    }
}
