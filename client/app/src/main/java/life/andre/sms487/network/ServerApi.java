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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import life.andre.sms487.events.MessagesStateChanged;
import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageContainer;
import life.andre.sms487.messages.MessageResendWorker;
import life.andre.sms487.messages.MessageStorage;
import life.andre.sms487.settings.AppSettings;
import life.andre.sms487.utils.BgTask;
import life.andre.sms487.utils.ValueThrottler;

public class ServerApi {
    public static final String TAG = "SAPI";
    public static final String MESSAGE_TYPE_SMS = "sms";
    public static final String MESSAGE_TYPE_NOTIFICATION = "notification";
    public static final long THROTTLE_DELAY = 500;
    public static final int MESSAGES_TO_SEND = 42;

    private static ServerApi instance;

    @NonNull
    private final RequestQueue requestQueue;
    @NonNull
    private final ValueThrottler<MessageContainer> throttler = new ValueThrottler<>(this::handleMessages, THROTTLE_DELAY);

    public static void init(@NonNull Context ctx) {
        instance = new ServerApi(ctx);
    }

    @NonNull
    public static ServerApi getInstance() {
        return Objects.requireNonNull(instance, "Not initialized");
    }

    private ServerApi(@NonNull Context ctx) {
        requestQueue = Volley.newRequestQueue(ctx);
    }

    public void addMessage(@NonNull MessageContainer msg) {
        throttler.handle(msg);
    }

    public void resendMessages() {
        BgTask.run(() -> {
            List<MessageContainer> messages = MessageStorage.getInstance().getNotSentMessages();
            if (messages.isEmpty()) {
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
            handleMessageBatches(messages);
            return null;
        });
    }

    private void handleMessageBatches(@NonNull List<MessageContainer> messages) {
        List<MessageContainer> toSend = new ArrayList<>();
        for (MessageContainer msg : messages) {
            toSend.add(msg);
            if (toSend.size() >= MESSAGES_TO_SEND) {
                addMessageList(toSend);
                toSend.clear();
            }
        }

        if (!toSend.isEmpty()) {
            addMessageList(toSend);
        }
    }

    private void addMessageList(@NonNull List<MessageContainer> messages) {
        List<Long> dbIds = MessageStorage.getInstance().addMessages(messages);

        JSONArray reqData = new JSONArray();
        for (MessageContainer msg : messages) {
            JSONObject item = createJsonRequestItem(msg);
            if (item == null) {
                continue;
            }
            reqData.put(item);
        }

        AppSettings appSettings = AppSettings.getInstance();
        String url = appSettings.getServerUrl();
        String key = appSettings.getServerKey();

        if (url.isEmpty() || key.isEmpty()) {
            Logger.w(TAG, "Server params are empty, skip sending");
            return;
        }

        this.requestQueue.add(new ApiAddMessageRequest(url, key, reqData.toString(), dbIds));
    }

    @Nullable
    private JSONObject createJsonRequestItem(@NonNull MessageContainer msg) {
        JSONObject item = new JSONObject();

        String text = msg.getBody();
        String messageType = msg.getMessageType();

        try {
            item.put("device_id", Build.MODEL)
                .put("message_type", messageType)
                .put("date_time", msg.getDateTime())
                .put("sms_date_time", msg.getSmsCenterDateTime())
                .put("tel", msg.getAddressFrom())
                .put("text", text);
        } catch (JSONException e) {
            Logger.e(TAG, e.toString());
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            return null;
        }

        logMessageSend(messageType, text);
        return item;
    }

    private void logMessageSend(@NonNull String messageType, @Nullable String text) {
        String logText = text != null ? text.replace('\n', ' ') : "null";

        int maxLogTextSize = 32;
        if (logText.length() > maxLogTextSize) {
            logText = logText.substring(0, maxLogTextSize) + "…";
        }

        String logLine = "Sending " + messageType + ": " + logText;
        Logger.i(TAG, logLine);
    }

    private static class ApiAddMessageRequest extends StringRequest {
        @NonNull
        private final String cookie;
        @NonNull
        private final String requestBody;

        ApiAddMessageRequest(@NonNull String url, @NonNull String key, @NonNull String requestBody, @NonNull List<Long> dbIds) {
            super(Request.Method.POST, url + "/add-sms", new ApiResponseListener(dbIds), new ApiErrorListener());
            this.cookie = "__Secure-Auth-Token=" + key;
            this.requestBody = requestBody;
        }

        @NonNull
        @Override
        public Map<String, String> getHeaders() {
            Map<String, String> headers = new HashMap<>();
            headers.put("Cookie", cookie);
            return headers;
        }

        @NonNull
        @Override
        public String getBodyContentType() {
            return "application/json; charset=utf-8";
        }

        @NonNull
        @Override
        public byte[] getBody() {
            return requestBody.getBytes(StandardCharsets.UTF_8);
        }
    }

    private static class ApiResponseListener implements Response.Listener<String> {
        @NonNull
        private final List<Long> dbIds;

        ApiResponseListener(@NonNull List<Long> dbIds) {
            this.dbIds = dbIds;
        }

        @Override
        public void onResponse(@Nullable String response) {
            markMessagesSent();

            if (response == null) {
                Logger.i(TAG, "Unknown request success");
                return;
            }

            logResponseDetails(response);
        }

        private void markMessagesSent() {
            BgTask.run(() -> {
                MessageStorage.getInstance().markSent(dbIds);
                EventBus.getDefault().post(new MessagesStateChanged());
                return null;
            });
        }

        private void logResponseDetails(@NonNull String response) {
            try {
                JSONObject resp = new JSONObject(response);

                String status = resp.optString("status", "UNK");
                int added = resp.optInt("added", -1);

                Logger.i(TAG, "Added: " + added + ": " + status);
            } catch (JSONException e) {
                Logger.e(TAG, e.toString());
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
            }
        }
    }

    private static class ApiErrorListener implements Response.ErrorListener {
        @Override
        public void onErrorResponse(@NonNull VolleyError error) {
            MessageResendWorker.scheduleOneTime();

            String finalErrorMessage = getFinalErrorMessage(error);
            Logger.e(TAG, finalErrorMessage);

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
    }
}
