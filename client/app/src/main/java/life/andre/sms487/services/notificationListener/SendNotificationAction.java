package life.andre.sms487.services.notificationListener;

import android.os.AsyncTask;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import life.andre.sms487.logging.Logger;

public class SendNotificationAction extends AsyncTask<SendNotificationParams, Void, Void> {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm Z",
            new Locale("UTC")
    );

    @Override
    protected Void doInBackground(SendNotificationParams... params) {
        if (params.length == 0) {
            Logger.w("SendNotificationAction", "Params length is 0");
            return null;
        }

        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        handleNotification(params[0]);

        return null;
    }

    void handleNotification(SendNotificationParams mainParams) {
        String dtFormatted = dateFormat.format(new Date(mainParams.postTime));

        long insertId = mainParams.messageStorage.addMessage(
                mainParams.deviceId, mainParams.appLabel, dtFormatted, mainParams.text
        );

        mainParams.smsApi.addSms(
                mainParams.deviceId, dtFormatted,
                mainParams.appLabel, mainParams.text, insertId
        );
    }
}
