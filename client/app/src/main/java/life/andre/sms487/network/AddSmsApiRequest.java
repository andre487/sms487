package life.andre.sms487.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import life.andre.sms487.logging.Logger;

class AddSmsApiRequest extends StringRequest {
    protected static class ApiResponseListener implements Response.Listener<String> {
        private List<SmsApi.RequestHandledListener> requestHandledListeners;
        private long dbId;

        ApiResponseListener(
                long dbId,
                List<SmsApi.RequestHandledListener> requestHandledListeners
        ) {
            this.requestHandledListeners = requestHandledListeners;
            this.dbId = dbId;
        }

        @Override
        public void onResponse(String response) {
            Logger.i("AddSmsApiRequest", "Response: " + response);

            for (SmsApi.RequestHandledListener listener : requestHandledListeners) {
                listener.onSuccess(dbId);
            }
        }
    }

    protected static class ApiErrorListener implements Response.ErrorListener {
        private List<SmsApi.RequestHandledListener> requestHandledListeners;
        private long dbId;

        ApiErrorListener(long dbId, List<SmsApi.RequestHandledListener> requestHandledListeners) {
            this.dbId = dbId;
            this.requestHandledListeners = requestHandledListeners;
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            String errorMessage = error.toString() + ": " + error.getMessage();
            Logger.w(
                    "AddSmsApiRequest",
                    "Response error: " + errorMessage
            );

            for (SmsApi.RequestHandledListener listener : requestHandledListeners) {
                listener.onError(dbId, errorMessage);
            }
        }
    }

    private Map<String, String> requestParams;
    private String cookie;

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
        this.cookie = "AUTH_TOKEN=" + serverKey;
    }

    AddSmsApiRequest(
            String serverUrl, String serverKey,
            Map<String, String> requestParams, long dbId
    ) {
        this(
                serverUrl, serverKey, requestParams,
                dbId, new ArrayList<SmsApi.RequestHandledListener>()
        );
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
