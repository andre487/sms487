package life.andre.sms487.activities.main;

import android.os.AsyncTask;

import java.util.List;

import life.andre.sms487.logging.Logger;

class GetLogsAction extends AsyncTask<Void, Void, List<String>> {
    @Override
    protected List<String> doInBackground(Void... params) {
        return Logger.getMessages();
    }
}
