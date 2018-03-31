package life.andre.sms487.logging;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@SuppressWarnings({"WeakerAccess", "unused"})
public class Logger {
    // TODO: use disk
    private static ConcurrentLinkedDeque<String> messages = new ConcurrentLinkedDeque<>();

    public static void v(String tag, String msg) {
        addMessageToQueue("VERBOSE", tag, msg);
        Log.v(tag, msg);
    }

    public static void d(String tag, String msg) {
        addMessageToQueue("DEBUG", tag, msg);
        Log.d(tag, msg);
    }

    public static void i(String tag, String msg) {
        addMessageToQueue("INFO", tag, msg);
        Log.i(tag, msg);
    }

    public static void w(String tag, String msg) {
        addMessageToQueue("WARN", tag, msg);
        Log.w(tag, msg);
    }

    public static void e(String tag, String msg) {
        addMessageToQueue("ERROR", tag, msg);
        Log.e(tag, msg);
    }

    public static List<String> getMessages() {
        ArrayList<String> result = new ArrayList<>();
        result.addAll(messages);
        return result;
    }

    private static void addMessageToQueue(String level, String tag, String msg) {
        String logLine = level + ": " + tag + ": " + msg;

        messages.addFirst(logLine);
    }
}
