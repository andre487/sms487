package life.andre.sms487.utils;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtil {
    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm Z", Locale.US);

    @NonNull
    public static String nowFormatted() {
        return formatDate(new Date());
    }

    @NonNull
    public static String formatDate(Date dt) {
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(dt);
    }

    @NonNull
    public static String formatDate(long ts) {
        return formatDate(new Date(ts));
    }
}
