package life.andre.sms487.smsListener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SmsListener extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        HandleMessageParams params = new HandleMessageParams(context, intent);

        new HandleMessageAction().execute(params);
    }
}
