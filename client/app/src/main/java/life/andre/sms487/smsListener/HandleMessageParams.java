package life.andre.sms487.smsListener;

import android.content.Context;
import android.content.Intent;

class HandleMessageParams {
    public Context context;
    public Intent intent;

    HandleMessageParams(Context context, Intent intent) {
        this.context = context;
        this.intent = intent;
    }
}
