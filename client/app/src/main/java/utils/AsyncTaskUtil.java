package utils;

import androidx.annotation.Nullable;

import life.andre.sms487.logging.Logger;

public class AsyncTaskUtil {
    @Nullable
    public static <T> T getParams(T[] params, String logTag) {
        if (params.length == 0) {
            Logger.w(logTag, "Params length is 0");
            return null;
        }
        return params[0];
    }

    public static <T> T getParams(T[] params) {
        return getParams(params, "AsyncTaskUtil");
    }
}
