package life.andre.sms487.logging;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class Logger {
    public static final int MAX_SIZE = 64;

    private static final ArrayList<String> messages = new ArrayList<>();

    public static void d(@NonNull String tag, @NonNull String msg) {
        Log.d(tag, msg);
        addMessage("DEBUG", tag, msg);
    }

    public static void i(@NonNull String tag, @NonNull String msg) {
        Log.i(tag, msg);
        addMessage("INFO", tag, msg);
    }

    public static void w(@NonNull String tag, @NonNull String msg) {
        Log.w(tag, msg);
        addMessage("WARN", tag, msg);
    }

    public static void e(@NonNull String tag, @NonNull String msg) {
        Log.e(tag, msg);
        addMessage("ERROR", tag, msg);
    }

    @NonNull
    public static List<String> getMessages() {
        return messages;
    }

    private static void addMessage(String level, String tag, String msg) {
        String logLine = level + ": " + tag + ": " + msg;

        synchronized (messages) {
            messages.add(0, logLine);
            while (messages.size() > MAX_SIZE) {
                messages.remove(messages.size() - 1);
            }
        }
    }
}
