package com.example.andre487.sms487.network;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class SmsApi {
    protected static class AddSmsApiRequest extends StringRequest {
        private HashMap<String, String> requestParams;

        AddSmsApiRequest(String serverUrl, HashMap<String, String> requestParams) {
            super(
                    Request.Method.POST,
                    serverUrl + "/add-sms",
                    new ApiResponseListener(),
                    new ApiErrorListener()
            );
            this.requestParams = requestParams;
        }

        @Override
        protected Map<String, String> getParams() {
            return requestParams;
        }
    }

    protected static class ApiResponseListener implements Response.Listener<String> {
        @Override
        public void onResponse(String response) {
            Log.i("SmsApi", "Response: " + response);
        }
    }

    protected static class ApiErrorListener implements Response.ErrorListener {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.w("SmsApi", "Response error: " + error.toString());
        }
    }

    private Context ctx;
    private String serverUrl;
    private String userName;
    private String serverKey;

    private RequestQueue requestQueue;

    public SmsApi(Context ctx, String serverUrl, String userName, String serverKey) {
        this.serverUrl = serverUrl;
        this.userName = userName;
        this.serverKey = serverKey;

        this.requestQueue = Volley.newRequestQueue(ctx);
    }

    public void addSms(String deviceId, String dateTime, String tel, String text) {
        Log.i(
                "SmsApi",
                "Sending SMS: " + deviceId + ", " + dateTime + ", " + tel + ", " + text
        );

        HashMap<String, String> requestParams = new HashMap<>();
        requestParams.put("device_id", deviceId);
        requestParams.put("date_time", dateTime);
        requestParams.put("tel", tel);
        requestParams.put("text", text);

        AddSmsApiRequest request = new AddSmsApiRequest(serverUrl, requestParams);

        this.requestQueue.add(request);
    }
}
