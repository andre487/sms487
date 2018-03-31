package life.andre.sms487.activities.main;

import android.os.AsyncTask;

import java.util.List;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.messageStorage.MessageContainer;

class GetMessagesAction extends AsyncTask<GetMessagesParams, Void, List<MessageContainer>> {
    @Override
    protected List<MessageContainer> doInBackground(GetMessagesParams... params) {
        if (params.length == 0) {
            Logger.w("GetMessagesAction", "Params length is 0");
            return null;
        }

        GetMessagesParams mainParams = params[0];

        return mainParams.messageStorage.getMessagesTail();
    }
}
