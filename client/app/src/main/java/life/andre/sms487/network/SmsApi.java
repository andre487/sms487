package life.andre.sms487.network;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import life.andre.sms487.logging.Logger;
import life.andre.sms487.preferences.AppSettings;

public class SmsApi {
    public final String MESSAGE_TYPE_SMS = "sms";
    public final String MESSAGE_TYPE_NOTIFICATION = "notification";

    private final AppSettings appSettings;

    private static final String logTag = "SmsApi";

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

    public void addSms(String deviceId, String dateTime, String smsCenterDateTime, String tel, String text, long dbId) {
        Logger.i(
                logTag,
                "Sending SMS: " + deviceId + ", " + dateTime + ", " + smsCenterDateTime + ", " + tel + ", " + text
        );
        addRequest(MESSAGE_TYPE_SMS, deviceId, dateTime, smsCenterDateTime, tel, text, dbId);
    }

    public void addNotification(String deviceId, String dateTime, String postDateTime, String tel, String text, long dbId) {
        Logger.i(
                logTag,
                "Sending Notification: " + deviceId + ", " + dateTime + ", " + ", " + postDateTime + ", " + tel + ", " + text
        );
        addRequest(MESSAGE_TYPE_NOTIFICATION, deviceId, dateTime, postDateTime, tel, text, dbId);
    }

    private void addRequest(
            String messageType, String deviceId, String dateTime, String smsCenterDateTime,
            String tel, String text, long dbId
    ) {
        String serverUrl = appSettings.getServerUrl();
        String serverKey = appSettings.getServerKey();

        if (serverUrl.length() == 0 || serverKey.length() == 0) {
            Logger.w(logTag, "Server params are empty, skip sending");
            return;
        }

        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("message_type", messageType);
        requestParams.put("device_id", deviceId);
        requestParams.put("date_time", dateTime);
        requestParams.put("sms_date_time", smsCenterDateTime);
        requestParams.put("tel", tel);
        requestParams.put("text", text);

        AddSmsApiRequest request = new AddSmsApiRequest(
                serverUrl, serverKey, requestParams, dbId,
                requestHandledListeners
        );

        this.requestQueue.add(request);
    }

    static class AddSmsApiRequest extends StringRequest {
        private final Map<String, String> requestParams;
        private final String cookie;

        AddSmsApiRequest(
                String serverUrl, String serverKey,
                Map<String, String> requestParams, long dbId,
                List<SmsApi.RequestHandledListener> requestHandledListeners
        ) {
            super(
                    Request.Method.POST,
                    serverUrl + "/add-sms",
                    new ApiResponseListener(dbId, requestHandledListeners),
                    new ApiErrorListener(dbId, requestHandledListeners)
            );

            this.requestParams = requestParams;
            this.cookie = "__Secure-Auth-Token=" + serverKey;
        }

        @Override
        public Map<String, String> getHeaders() {
            Map<String, String> headers = new HashMap<>();

            headers.put("Cookie", cookie);

            return headers;
        }

        @Override
        protected Map<String, String> getParams() {
            return requestParams;
        }
    }

    static class ApiResponseListener implements Response.Listener<String> {
        private final List<SmsApi.RequestHandledListener> requestHandledListeners;
        private final long dbId;

        ApiResponseListener(
                long dbId,
                List<SmsApi.RequestHandledListener> requestHandledListeners
        ) {
            this.requestHandledListeners = requestHandledListeners;
            this.dbId = dbId;
        }

        @Override
        public void onResponse(String response) {
            if (response == null) {
                response = "Unknown response";
            }
            Logger.i("AddSmsApiRequest", "Response: " + response);

            for (SmsApi.RequestHandledListener listener : requestHandledListeners) {
                listener.onSuccess(dbId);
            }
        }
    }

    static class ApiErrorListener implements Response.ErrorListener {
        private final List<SmsApi.RequestHandledListener> requestHandledListeners;
        private final long dbId;

        ApiErrorListener(long dbId, List<SmsApi.RequestHandledListener> requestHandledListeners) {
            this.dbId = dbId;
            this.requestHandledListeners = requestHandledListeners;
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            String errorMessage = "Unknown network error";
            if (error.networkResponse != null) {
                errorMessage = error.toString() + ": " +
                        error.networkResponse.statusCode + ": " +
                        new String(error.networkResponse.data, StandardCharsets.UTF_8);
            }

            Logger.e(logTag, "Response error: " + errorMessage);

            for (SmsApi.RequestHandledListener listener : requestHandledListeners) {
                listener.onError(dbId, errorMessage);
            }
        }
    }
}
