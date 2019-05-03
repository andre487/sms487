package life.andre.sms487.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import life.andre.sms487.logging.Logger;

public class SmsApi {
    public interface RequestHandledListener {
        void onSuccess(long dbId);

        void onError(long errorMessage, String dbId);
    }

    private String serverUrl;
    private String serverKey;

    private RequestQueue requestQueue;
    private List<RequestHandledListener> requestHandledListeners = new ArrayList<>();

    public SmsApi(Context ctx, String serverUrl, String serverKey) {
        this.serverUrl = serverUrl;
        this.serverKey = serverKey;

        this.requestQueue = Volley.newRequestQueue(ctx);
    }

    public void addRequestHandledListener(RequestHandledListener listener) {
        requestHandledListeners.add(listener);
    }

    public void addSms(String deviceId, String dateTime, String tel, String text, long dbId) {
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
                serverUrl, serverKey, requestParams, dbId,
                requestHandledListeners
        );

        this.requestQueue.add(request);
    }

    @SuppressWarnings("unused")
    public void addSms(String deviceId, String dateTime, String tel, String text) {
        addSms(deviceId, dateTime, tel, text, 0);
    }
}
