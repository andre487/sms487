package life.andre.sms487.services.smsHandler;

import android.os.AsyncTask;
import androidx.annotation.Nullable;

import life.andre.sms487.logging.Logger;

public class MarkSmsSentAction extends AsyncTask<MarkSmsSentParams, Void, Void> {
    @Override
    @Nullable
    protected Void doInBackground(MarkSmsSentParams... params) {
        if (params.length == 0) {
            Logger.w("MarkSmsSentAction", "Params length is 0");
            return null;
        }
        MarkSmsSentParams mainParams = params[0];

        mainParams.messageStorage.markSent(mainParams.dbId);

        return null;
    }
}
