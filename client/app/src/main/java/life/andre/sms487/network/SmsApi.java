package life.andre.sms487.network;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import life.andre.sms487.logging.Logger;

public class SmsApi {
    private String serverUrl;
    private String serverKey;

    private RequestQueue requestQueue;

    public SmsApi(Context ctx, String serverUrl, String serverKey) {
        this.serverUrl = serverUrl;
        this.serverKey = serverKey;

        this.requestQueue = Volley.newRequestQueue(ctx);
    }

    public void addSms(String deviceId, String dateTime, String tel, String text) {
        Logger.i(
                "SmsApi",
                "Sending SMS: " + deviceId + ", " + dateTime + ", " + tel + ", " + text
        );

        if (serverUrl.length() == 0 || serverKey.length() == 0) {
            Logger.i("SmsApi", "Server params are empty, skip sending");
            return;
        }

        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("device_id", deviceId);
        requestParams.put("date_time", dateTime);
        requestParams.put("tel", tel);
        requestParams.put("text", text);

        AddSmsApiRequest request = new AddSmsApiRequest(
                serverUrl, serverKey, requestParams
        );

        this.requestQueue.add(request);
    }
}
