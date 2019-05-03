package life.andre.sms487.network;

import java.util.HashMap;
import java.util.Map;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import life.andre.sms487.logging.Logger;

class AddSmsApiRequest extends StringRequest {
    protected static class ApiResponseListener implements Response.Listener<String> {
        @Override
        public void onResponse(String response) {
            Logger.i("AddSmsApiRequest", "Response: " + response);
        }
    }

    protected static class ApiErrorListener implements Response.ErrorListener {
        @Override
        public void onErrorResponse(VolleyError error) {
            Logger.w(
                    "AddSmsApiRequest",
                    "Response error: " + error.toString() + ": " + error.getMessage()
            );
        }
    }

    private Map<String, String> requestParams;
    private String cookie;

    AddSmsApiRequest(
            String serverUrl, String serverKey,
            Map<String, String> requestParams
    ) {
        super(
                Request.Method.POST,
                serverUrl + "/add-sms",
                new ApiResponseListener(),
                new ApiErrorListener()
        );

        this.requestParams = requestParams;
        this.cookie = "AUTH_TOKEN=" + serverKey;
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
