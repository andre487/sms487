package life.andre.sms487.network;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import life.andre.sms487.messages.MessageContainer;
import life.andre.sms487.messages.MessageStorage;
import life.andre.sms487.system.AppSettings;
import life.andre.sms487.utils.AsyncTaskUtil;

public class ServerApi {
    public static final String TAG = "ServerApi";
    public static final String MESSAGE_TYPE_SMS = "sms";
    public static final String MESSAGE_TYPE_NOTIFICATION = "notification";

    private static final List<RequestHandledListener> requestHandledListeners = new ArrayList<>();

    private final AppSettings appSettings;
    @NonNull
    private final MessageStorage messageStorage;
    @NonNull
    private final RequestQueue requestQueue;

    public interface RequestHandledListener {
        void onSuccess();

        void onError(String errorMessage);
    }

    public ServerApi(@NonNull Context ctx, AppSettings appSettings) {
        this.appSettings = appSettings;
        this.requestQueue = Volley.newRequestQueue(ctx);
        messageStorage = new MessageStorage(ctx);
    }

    public void addRequestHandledListener(RequestHandledListener listener) {
        requestHandledListeners.add(listener);
    }

    public void removeRequestHandledListener(RequestHandledListener listener) {
        requestHandledListeners.remove(listener);
    }

    public void addMessage(@NonNull MessageContainer msg) {
        long dbId = msg.getDbId();
        if (dbId == 0) {
            dbId = messageStorage.addMessage(msg);
        }

        String messageType = msg.getMessageType();
        String dateTime = msg.getDateTime();
        String postDateTime = msg.getSmsCenterDateTime();
        String tel = msg.getAddressFrom();
        String text = msg.getBody();

        String logText = text != null ? text.replace('\n', ' ') : "null";

        int maxLogTextSize = 32;
        if (logText.length() > maxLogTextSize) {
            logText = logText.substring(0, maxLogTextSize) + "…";
        }

        String logLine = "Sending " + messageType + ": " + logText;
        Logger.i(TAG, logLine);

        addRequest(messageType, dateTime, postDateTime, tel, text, dbId);
    }

    public void resendMessages() {
        ResendMessagesParams params = new ResendMessagesParams(messageStorage, this);
        new ResendMessagesAction().execute(params);
    }

    private void addRequest(
            String messageType, String dateTime, @Nullable String smsCenterDateTime,
            String tel, String text, long dbId
    ) {
        String serverUrl = appSettings.getServerUrl();
        String serverKey = appSettings.getServerKey();

        if (serverUrl == null || serverUrl.length() == 0 || serverKey == null || serverKey.length() == 0) {
            Logger.w(TAG, "Server params are empty, skip sending");
            return;
        }

        if (smsCenterDateTime == null) {
            smsCenterDateTime = dateTime;
        }

        Map<String, String> requestParams = new HashMap<>();

        requestParams.put("device_id", Build.MODEL);
        requestParams.put("message_type", messageType);
        requestParams.put("date_time", dateTime);
        requestParams.put("sms_date_time", smsCenterDateTime);
        requestParams.put("tel", tel);
        requestParams.put("text", text);

        AddRequest request = new AddRequest(serverUrl, serverKey, requestParams, dbId, messageStorage);

        this.requestQueue.add(request);
    }

    static class AddRequest extends StringRequest {
        private final Map<String, String> requestParams;
        @NonNull
        private final String cookie;

        AddRequest(
                String serverUrl, String serverKey,
                Map<String, String> requestParams, long dbId, MessageStorage messageStorage
        ) {
            super(
                    Request.Method.POST,
                    serverUrl + "/add-sms",
                    new ApiResponseListener(dbId, messageStorage),
                    new ApiErrorListener(dbId)
            );

            this.requestParams = requestParams;
            this.cookie = "__Secure-Auth-Token=" + serverKey;
        }

        @NonNull
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
        private final long dbId;
        private final MessageStorage messageStorage;

        ApiResponseListener(long dbId, MessageStorage messageStorage) {
            this.dbId = dbId;
            this.messageStorage = messageStorage;
        }

        @Override
        public void onResponse(@Nullable String response) {
            if (response == null) {
                response = "Unknown response";
            }
            Logger.i(TAG, "Response: " + response);

            RequestSuccessParams params = new RequestSuccessParams(dbId, messageStorage);
            new RunRequestSuccessHandlers().execute(params);
        }
    }

    static class ApiErrorListener implements Response.ErrorListener {
        private final long dbId;

        ApiErrorListener(long dbId) {
            this.dbId = dbId;
        }

        @Override
        public void onErrorResponse(@NonNull VolleyError error) {
            String errorMessage = error.toString();
            if (error.networkResponse != null) {
                errorMessage = error.toString() + ": " +
                        error.networkResponse.statusCode + ": " +
                        new String(error.networkResponse.data, StandardCharsets.UTF_8);
            }

            Logger.e(TAG, errorMessage);

            RequestErrorParams params = new RequestErrorParams(dbId, errorMessage);
            new RunRequestErrorHandlers().execute(params);
        }
    }

    static class RequestSuccessParams {
        final long dbId;
        final MessageStorage messageStorage;

        RequestSuccessParams(long dbId, MessageStorage messageStorage) {
            this.dbId = dbId;
            this.messageStorage = messageStorage;
        }
    }

    static class RunRequestSuccessHandlers extends AsyncTask<RequestSuccessParams, Void, Void> {
        @Nullable
        @Override
        protected Void doInBackground(@NonNull RequestSuccessParams... params) {
            RequestSuccessParams mainParams = AsyncTaskUtil.getParams(params, TAG);
            if (mainParams == null) {
                return null;
            }

            mainParams.messageStorage.markSent(mainParams.dbId);

            for (ServerApi.RequestHandledListener listener : requestHandledListeners) {
                listener.onSuccess();
            }

            return null;
        }
    }

    static class RequestErrorParams {
        final long dbId;
        final String errorMessage;

        RequestErrorParams(long dbId, String errorMessage) {
            this.dbId = dbId;
            this.errorMessage = errorMessage;
        }
    }

    static class RunRequestErrorHandlers extends AsyncTask<RequestErrorParams, Void, Void> {
        @Nullable
        @Override
        protected Void doInBackground(@NonNull RequestErrorParams... params) {
            RequestErrorParams mainParams = AsyncTaskUtil.getParams(params, TAG);
            if (mainParams == null) {
                return null;
            }

            for (ServerApi.RequestHandledListener listener : requestHandledListeners) {
                listener.onError(mainParams.errorMessage);
            }

            return null;
        }
    }

    static class ResendMessagesParams {
        final MessageStorage messageStorage;
        final ServerApi serverApi;

        ResendMessagesParams(MessageStorage messageStorage, ServerApi serverApi) {
            this.messageStorage = messageStorage;
            this.serverApi = serverApi;
        }
    }

    static class ResendMessagesAction extends AsyncTask<ResendMessagesParams, Void, Void> {
        @Nullable
        @Override
        protected Void doInBackground(@NonNull ResendMessagesParams... params) {
            ResendMessagesParams mainParams = AsyncTaskUtil.getParams(params, TAG);
            if (mainParams == null) {
                return null;
            }

            List<MessageContainer> messages = mainParams.messageStorage.getNotSentMessages();
            if (messages.size() == 0) {
                return null;
            }

            int notSentCount = messages.size();
            Logger.i(TAG, "Resend: try to resend " + notSentCount + " messages");

            for (MessageContainer message : messages) {
                mainParams.serverApi.addMessage(message);
            }

            return null;
        }
    }
}
