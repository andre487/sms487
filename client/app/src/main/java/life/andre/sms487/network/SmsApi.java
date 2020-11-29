package life.andre.sms487.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.preferences.AppSettings;

public class SmsApi {
    private final AppSettings appSettings;

    public interface RequestHandledListener {
        void onSuccess(long dbId);

        void onError(long errorMessage, String dbId);
    }

    private final RequestQueue requestQueue;
    private final List<RequestHandledListener> requestHandledListeners = new ArrayList<>();

    public SmsApi(Context ctx, AppSettings appSettings) {
        this.appSettings = appSettings;

        this.requestQueue = Volley.newRequestQueue(ctx);
    }

    public void addRequestHandledListener(RequestHandledListener listener) {
        requestHandledListeners.add(listener);
    }

    public void removeRequestHandledListener(RequestHandledListener listener) {
        requestHandledListeners.remove(listener);
    }

    public void addSms(String deviceId, String dateTime, String tel, String text, long dbId) {
        Logger.i(
                "SmsApi",
                "Sending SMS: " + deviceId + ", " + dateTime + ", " + tel + ", " + text
        );

        String serverUrl = appSettings.getServerUrl();
        String serverKey = appSettings.getServerKey();

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
}
