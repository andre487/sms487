package life.andre.sms487.services.notificationListener;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import life.andre.sms487.logging.Logger;

public class SendNotificationAction extends AsyncTask<SendNotificationParams, Void, Void> {
    private SimpleDateFormat dateFormat;

    @SuppressLint("SimpleDateFormat")
    @Override
    protected Void doInBackground(SendNotificationParams... params) {
        if (params.length == 0) {
            Logger.w("SendNotificationAction", "Params length is 0");
            return null;
        }

        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm Z");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        handleNotification(params[0]);

        return null;
    }

    void handleNotification(SendNotificationParams mainParams) {
        String curTime = dateFormat.format(new Date());
        String postTime = dateFormat.format(new Date(mainParams.postTime));

        long insertId = mainParams.messageStorage.addMessage(
                mainParams.deviceId, mainParams.appLabel, curTime, postTime, mainParams.text
        );

        mainParams.smsApi.addNotification(
                mainParams.deviceId, curTime, postTime,
                mainParams.appLabel, mainParams.text, insertId
        );
    }
}
