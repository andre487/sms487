package life.andre.sms487.network;

import java.util.HashMap;
import java.util.Map;

import android.util.Base64;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import life.andre.sms487.logging.Logger;

class AddSmsApiRequest extends StringRequest {
    protected static class ApiResponseListener implements Response.Listener<String> {
        @Override
        public void onResponse(String response) {
            Logger.i("SmsApi", "Response: " + response);
        }
    }

    protected static class ApiErrorListener implements Response.ErrorListener {
        @Override
        public void onErrorResponse(VolleyError error) {
            Logger.w("SmsApi", "Response error: " + error.toString());
        }
    }

    private Map<String, String> requestParams;
    private String authHeaderString;

    AddSmsApiRequest(
            String serverUrl, String userName, String serverKey,
            Map<String, String> requestParams
    ) {
        super(
                Request.Method.POST,
                serverUrl + "/add-sms",
                new ApiResponseListener(),
                new ApiErrorListener()
        );

        this.requestParams = requestParams;

        byte[] authData = (userName + ':' + serverKey).getBytes();
        authHeaderString = "Basic " + Base64.encodeToString(authData, Base64.DEFAULT);
    }

    @Override
    public Map<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();

        headers.put("Authorization", authHeaderString);

        return headers;
    }

    @Override
    protected Map<String, String> getParams() {
        return requestParams;
    }
}
