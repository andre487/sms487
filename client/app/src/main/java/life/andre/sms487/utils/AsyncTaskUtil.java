package life.andre.sms487.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import life.andre.sms487.logging.Logger;

public class AsyncTaskUtil {
    public static final String TAG = AsyncTaskUtil.class.getSimpleName();

    @Nullable
    public static <T> T getParams(@NonNull T[] params, @NonNull String logTag) {
        if (params.length == 0) {
            Logger.w(logTag, "Params length is 0");
            return null;
        }
        return params[0];
    }

    @Nullable
    public static <T> T getParams(@NonNull T[] params) {
        return getParams(params, TAG);
    }
}
