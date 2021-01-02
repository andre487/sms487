package life.andre.sms487.network;

import android.content.Context;
import android.os.AsyncTask;

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

public class SmsApi {
    public static final String MESSAGE_TYPE_SMS = "sms";
    public static final String MESSAGE_TYPE_NOTIFICATION = "notification";

    private static final String logTag = "SmsApi";
    private static final List<RequestHandledListener> requestHandledListeners = new ArrayList<>();

    private final AppSettings appSettings;
    private final MessageStorage messageStorage;
    private final RequestQueue requestQueue;

    public interface RequestHandledListener {
        void onSuccess(long dbId);

        void onError(long dbId, String errorMessage);
    }

    public SmsApi(Context ctx, AppSettings appSettings) {
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

    public void addSms(String deviceId, String dateTime, String smsCenterDateTime, String tel, String text, long dbId) {
        Logger.i(
                logTag,
                "Sending SMS: " + deviceId + ", " + dateTime + ", " + smsCenterDateTime + ", " + tel + ", " + text
        );
        addRequest(MESSAGE_TYPE_SMS, deviceId, dateTime, smsCenterDateTime, tel, text, dbId);
    }

    public void addSms(MessageContainer msg) {
        long dbId = msg.getDbId();
        if (dbId == 0) {
            dbId = messageStorage.addMessage(msg);
        }

        addSms(
                msg.getDeviceId(), msg.getDateTime(), msg.getSmsCenterDateTime(),
                msg.getAddressFrom(), msg.getBody(), dbId
        );
    }

    public void addNotification(String deviceId, String dateTime, String postDateTime, String tel, String text, long dbId) {
        Logger.i(
                logTag,
                "Sending Notification: " + deviceId + ", " + dateTime + ", " + ", " + postDateTime + ", " + tel + ", " + text
        );
        addRequest(MESSAGE_TYPE_NOTIFICATION, deviceId, dateTime, postDateTime, tel, text, dbId);
    }

    public void addNotification(MessageContainer msg) {
        long dbId = msg.getDbId();
        if (dbId == 0) {
            dbId = messageStorage.addMessage(msg);
        }

        addNotification(
                msg.getDeviceId(), msg.getDateTime(), msg.getSmsCenterDateTime(),
                msg.getAddressFrom(), msg.getBody(), dbId
        );
    }

    public void resendMessages() {
        ResendMessagesParams params = new ResendMessagesParams(messageStorage, this);
        new ResendMessagesAction().execute(params);
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

        if (smsCenterDateTime == null) {
            smsCenterDateTime = dateTime;
        }

        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("message_type", messageType);
        requestParams.put("device_id", deviceId);
        requestParams.put("date_time", dateTime);
        requestParams.put("sms_date_time", smsCenterDateTime);
        requestParams.put("tel", tel);
        requestParams.put("text", text);

        AddSmsApiRequest request = new AddSmsApiRequest(
                serverUrl, serverKey, requestParams, dbId, messageStorage,
                requestHandledListeners
        );

        this.requestQueue.add(request);
    }

    static class AddSmsApiRequest extends StringRequest {
        private final Map<String, String> requestParams;
        private final String cookie;

        AddSmsApiRequest(
                String serverUrl, String serverKey,
                Map<String, String> requestParams, long dbId, MessageStorage messageStorage,
                List<SmsApi.RequestHandledListener> requestHandledListeners
        ) {
            super(
                    Request.Method.POST,
                    serverUrl + "/add-sms",
                    new ApiResponseListener(dbId, messageStorage, requestHandledListeners),
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
        private final MessageStorage messageStorage;

        ApiResponseListener(
                long dbId, MessageStorage messageStorage,
                List<SmsApi.RequestHandledListener> requestHandledListeners
        ) {
            this.requestHandledListeners = requestHandledListeners;
            this.dbId = dbId;
            this.messageStorage = messageStorage;
        }

        @Override
        public void onResponse(String response) {
            if (response == null) {
                response = "Unknown response";
            }
            Logger.i("AddSmsApiRequest", "Response: " + response);

            RequestSuccessParams params = new RequestSuccessParams(dbId, messageStorage, requestHandledListeners);
            new RunRequestSuccessHandlers().execute(params);
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

            RequestErrorParams params = new RequestErrorParams(dbId, errorMessage, requestHandledListeners);
            new RunRequestErrorHandlers().execute(params);
        }
    }

    static class RequestSuccessParams {
        long dbId;
        MessageStorage messageStorage;
        List<RequestHandledListener> requestHandledListeners;

        RequestSuccessParams(long dbId, MessageStorage messageStorage, List<SmsApi.RequestHandledListener> requestHandledListeners) {
            this.dbId = dbId;
            this.messageStorage = messageStorage;
            this.requestHandledListeners = requestHandledListeners;
        }
    }

    static class RunRequestSuccessHandlers extends AsyncTask<RequestSuccessParams, Void, Void> {
        @Override
        protected Void doInBackground(RequestSuccessParams... params) {
            RequestSuccessParams mainParams = AsyncTaskUtil.getParams(params, logTag);
            if (mainParams == null) {
                return null;
            }

            mainParams.messageStorage.markSent(mainParams.dbId);

            for (SmsApi.RequestHandledListener listener : mainParams.requestHandledListeners) {
                listener.onSuccess(mainParams.dbId);
            }

            return null;
        }
    }

    static class RequestErrorParams {
        long dbId;
        String errorMessage;
        List<SmsApi.RequestHandledListener> requestHandledListeners;

        RequestErrorParams(long dbId, String errorMessage, List<SmsApi.RequestHandledListener> requestHandledListeners) {
            this.dbId = dbId;
            this.errorMessage = errorMessage;
            this.requestHandledListeners = requestHandledListeners;
        }
    }

    static class RunRequestErrorHandlers extends AsyncTask<RequestErrorParams, Void, Void> {
        @Override
        protected Void doInBackground(RequestErrorParams... params) {
            RequestErrorParams mainParams = AsyncTaskUtil.getParams(params, logTag);
            if (mainParams == null) {
                return null;
            }

            for (SmsApi.RequestHandledListener listener : mainParams.requestHandledListeners) {
                listener.onError(mainParams.dbId, mainParams.errorMessage);
            }

            return null;
        }
    }

    static class ResendMessagesParams {
        MessageStorage messageStorage;
        SmsApi smsApi;

        ResendMessagesParams(MessageStorage messageStorage, SmsApi smsApi) {
            this.messageStorage = messageStorage;
            this.smsApi = smsApi;
        }
    }

    static class ResendMessagesAction extends AsyncTask<ResendMessagesParams, Void, Void> {
        @Override
        protected Void doInBackground(ResendMessagesParams... params) {
            ResendMessagesParams mainParams = AsyncTaskUtil.getParams(params, logTag);
            if (mainParams == null) {
                return null;
            }

            List<MessageContainer> messages = mainParams.messageStorage.getNotSentMessages();

            // TODO: notifications?
            for (MessageContainer message : messages) {
                mainParams.smsApi.addSms(message);
            }

            return null;
        }
    }
}
