package life.andre.sms487.network;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.greenrobot.eventbus.EventBus;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import life.andre.sms487.events.MessagesStateChanged;
import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageContainer;
import life.andre.sms487.messages.MessageResendWorker;
import life.andre.sms487.messages.MessageStorage;
import life.andre.sms487.settings.AppSettings;
import life.andre.sms487.utils.BgTask;
import life.andre.sms487.utils.StringUtil;
import life.andre.sms487.utils.ValueThrottler;
import life.andre.sms487.views.Toaster;

public class ServerApi {
    public static final String TAG = "ServerApi";
    public static final String MESSAGE_TYPE_SMS = "sms";
    public static final String MESSAGE_TYPE_NOTIFICATION = "notification";
    public static final long THROTTLE_DELAY = 500;

    @NonNull
    private final AppSettings appSettings;
    @NonNull
    private final MessageStorage messageStorage;
    @NonNull
    private final RequestQueue requestQueue;
    @NonNull
    private final ValueThrottler<MessageContainer> throttler = new ValueThrottler<>(this::handleMessages, THROTTLE_DELAY);

    public ServerApi(@NonNull Context ctx) {
        requestQueue = Volley.newRequestQueue(ctx);
        appSettings = new AppSettings(ctx);
        messageStorage = new MessageStorage(ctx);
    }

    public void addMessage(@NonNull MessageContainer msg) {
        throttler.handle(msg);
    }

    public void resendMessages() {
        BgTask.run(() -> {
            List<MessageContainer> messages = messageStorage.getNotSentMessages();
            if (messages.size() == 0) {
                return null;
            }

            int notSentCount = messages.size();
            Logger.i(TAG, "Resend: try to resend " + notSentCount + " messages");

            for (MessageContainer message : messages) {
                throttler.handle(message);
            }

            return null;
        });
    }

    private void handleMessages(@NonNull List<MessageContainer> messages) {
        BgTask.run(() -> {
            // TODO: make batch request
            for (MessageContainer msg : messages) {
                addMessageSlow(msg);
            }
            return null;
        });
    }

    private void addMessageSlow(@NonNull MessageContainer msg) {
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

        logMessageSend(messageType, logText);

        addRequest(messageType, dateTime, postDateTime, tel, text, dbId);
    }

    private void addRequest(
            String messageType, String dateTime, @Nullable String smsCenterDateTime,
            String tel, String text, long dbId
    ) {
        String serverUrl = appSettings.getServerUrl();
        String serverKey = appSettings.getServerKey();

        if (serverUrl.length() == 0 || serverKey.length() == 0) {
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

    private void logMessageSend(String messageType, String logText) {
        int maxLogTextSize = 32;
        if (logText.length() > maxLogTextSize) {
            logText = logText.substring(0, maxLogTextSize) + "…";
        }

        String logLine = "Sending " + messageType + ": " + logText;
        Logger.i(TAG, logLine);
    }

    private static class AddRequest extends StringRequest {
        private final Map<String, String> requestParams;
        @NonNull
        private final String cookie;

        AddRequest(String url, String key, Map<String, String> params, long dbId, MessageStorage msgStorage) {
            super(
                    Request.Method.POST,
                    url + "/add-sms",
                    new ApiResponseListener(dbId, msgStorage),
                    new ApiErrorListener()
            );
            this.requestParams = params;
            this.cookie = "__Secure-Auth-Token=" + key;
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

    private static class ApiResponseListener implements Response.Listener<String> {
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

            BgTask.run(() -> {
                messageStorage.markSent(dbId);
                EventBus.getDefault().post(new MessagesStateChanged());
                return null;
            });
        }
    }

    private static class ApiErrorListener implements Response.ErrorListener {
        public static final long MESSAGE_DELAY = 250;
        // TODO: move to Toaster
        private final ValueThrottler<String> msgThrottler = new ValueThrottler<>(this::showErrorMessage, MESSAGE_DELAY);

        @Override
        public void onErrorResponse(@NonNull VolleyError error) {
            MessageResendWorker.scheduleOneTime();

            String finalErrorMessage = getFinalErrorMessage(error);
            Logger.e(TAG, finalErrorMessage);
            msgThrottler.handle(finalErrorMessage);

            EventBus.getDefault().post(new MessagesStateChanged());
        }

        @NonNull
        private String getFinalErrorMessage(@NonNull VolleyError error) {
            StringBuilder err = new StringBuilder(error.toString());
            if (error.networkResponse != null) {
                err.append(": ")
                        .append(error.networkResponse.statusCode).append(": ")
                        .append(new String(error.networkResponse.data, StandardCharsets.UTF_8));
            }
            return err.toString();
        }

        private void showErrorMessage(@NonNull List<String> messages) {
            String msg = StringUtil.joinUnique(messages, "\n");
            Toaster.showMessage(msg);
        }
    }
}
