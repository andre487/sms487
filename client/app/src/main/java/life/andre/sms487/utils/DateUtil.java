package life.andre.sms487.utils;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtil {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm Z", Locale.US);

    @NonNull
    public static String nowFormatted() {
        return formatDate(new Date());
    }

    @NonNull
    public static String formatDate(@NonNull Date dt) {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        return DATE_FORMAT.format(dt);
    }

    @NonNull
    public static String formatDate(long ts) {
        return formatDate(new Date(ts));
    }
}
