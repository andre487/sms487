package life.andre.sms487.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.method.ScrollingMovementMethod;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;

import java.util.List;
import java.util.concurrent.ExecutionException;

import life.andre.sms487.R;
import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageCleanupWorker;
import life.andre.sms487.messages.MessageContainer;
import life.andre.sms487.messages.MessageResendWorker;
import life.andre.sms487.messages.MessageStorage;
import life.andre.sms487.network.SmsApi;
import life.andre.sms487.system.AppSettings;
import life.andre.sms487.system.PermissionsChecker;
import life.andre.sms487.utils.AsyncTaskUtil;

public class MainActivity extends AppCompatActivity {
    private final PermissionsChecker permissionsChecker = new PermissionsChecker(this);
    private final static String logTag = "MainActivity";

    private final Handler logUpdateHandler = new Handler();
    private final LogUpdater logUpdater = new LogUpdater(this);

    private MessageStorage messageStorage;
    private SmsApi smsApi;
    private AppSettings appSettings;
    private SmsApi.RequestHandledListener smsRequestListener;

    private AppCompatEditText serverKeyInput;
    private AppCompatEditText serverUrlInput;
    private AppCompatTextView messagesField;
    private AppCompatTextView logsField;

    private AppCompatButton saveServerKeyButton;
    private AppCompatButton saveServerUrlButton;

    private AppCompatCheckBox sendSmsCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startServiceTasks();

        messageStorage = new MessageStorage(this);
        appSettings = new AppSettings(this);
        smsApi = new SmsApi(this, appSettings);

        smsRequestListener = new SmsRequestListener(this);

        findViewComponents();
        bindEvents();
    }

    @Override
    protected void onStart() {
        super.onStart();
        permissionsChecker.checkPermissions();

        showServerUrl();
        showServerKey();
        showNeedSendSms();
        renewMessagesFromDb();
        enableLogAutoRenew();

        smsApi.addRequestHandledListener(smsRequestListener);
        smsApi.resendMessages();
    }

    @Override
    protected void onStop() {
        smsApi.removeRequestHandledListener(smsRequestListener);
        disableLogAutoRenew();
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String action = intent.getStringExtra("action");
        if (action == null) {
            return;
        }

        switch (action) {
            case "renewMessages":
                renewMessagesFromDb();
                break;
            case "toastMessage":
                String message = intent.getStringExtra("message");
                if (message != null) {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private void startServiceTasks() {
        MessageCleanupWorker.schedule();
        MessageResendWorker.schedule();
    }

    private void findViewComponents() {
        serverKeyInput = findViewById(R.id.serverKeyInput);
        serverUrlInput = findViewById(R.id.serverUrlInput);
        messagesField = findViewById(R.id.messagesField);
        logsField = findViewById(R.id.logsField);

        saveServerUrlButton = findViewById(R.id.serverUrlSave);
        saveServerKeyButton = findViewById(R.id.serverKeySave);

        sendSmsCheckBox = findViewById(R.id.sendSmsCheckBox);
    }

    private void bindEvents() {
        MainActivity mainActivity = this;

        saveServerUrlButton.setOnClickListener(v -> mainActivity.saveServerUrl());
        saveServerKeyButton.setOnClickListener(v -> mainActivity.saveServerKey());
        sendSmsCheckBox.setOnCheckedChangeListener((v, c) -> mainActivity.saveNeedSendSms());

        messagesField.setMovementMethod(new ScrollingMovementMethod());

        logsField.setMovementMethod(new ScrollingMovementMethod());
        logsField.setOnFocusChangeListener((v, f) -> {
            if (f) {
                disableLogAutoRenew();
            } else {
                enableLogAutoRenew();
            }
        });
    }

    void renewMessagesFromDb() {
        final List<MessageContainer> messages = getMessages();
        if (messages == null) {
            Logger.w(logTag, "Messages are null");
            return;
        }
        showMessages(messages);
    }

    void showLogsFromLogger() {
        if (logsField == null) {
            return;
        }

        StringBuilder logsString = new StringBuilder();
        for (String logLine : Logger.getMessages()) {
            logsString.append(logLine);
            logsString.append('\n');
        }

        logsField.setText(logsString.toString().trim());
    }

    void saveServerKey() {
        if (serverKeyInput == null) {
            return;
        }

        Editable serverKeyText = serverKeyInput.getText();
        if (serverKeyText != null) {
            appSettings.saveServerKey(serverKeyText.toString());
        }
    }

    public void showServerUrl() {
        if (serverUrlInput == null) {
            return;
        }

        serverUrlInput.setText(appSettings.getServerUrl());
    }

    void saveServerUrl() {
        if (serverUrlInput == null) {
            return;
        }

        Editable serverUrlText = serverUrlInput.getText();
        if (serverUrlText != null) {
            appSettings.saveServerUrl(serverUrlText.toString());
        }
    }

    public void showServerKey() {
        if (serverKeyInput == null) {
            return;
        }

        serverKeyInput.setText(appSettings.getServerKey());
    }

    private List<MessageContainer> getMessages() {
        GetMessagesParams params = new GetMessagesParams(messageStorage);
        GetMessagesAction action = new GetMessagesAction();
        action.execute(params);

        try {
            return action.get();
        } catch (InterruptedException | ExecutionException e) {
            Logger.w(logTag, "Get messages error: " + e.toString());
            e.printStackTrace();
            return null;
        }
    }

    private void showMessages(List<MessageContainer> messages) {
        if (messagesField == null) {
            return;
        }

        StringBuilder messagesString = new StringBuilder();

        for (MessageContainer message : messages) {
            messagesString.append(message.getAddressFrom());
            messagesString.append('\t');
            messagesString.append(message.getDateTime());
            messagesString.append("\nSent: ");
            messagesString.append(message.isSent() ? "yes" : "no");
            messagesString.append('\n');

            messagesString.append(message.getBody());
            messagesString.append("\n\n");
        }

        messagesField.setText(messagesString.toString().trim());
    }

    private void enableLogAutoRenew() {
        if (!logUpdateHandler.hasCallbacks(logUpdater)) {
            logUpdateHandler.post(logUpdater);
        }
    }

    private void disableLogAutoRenew() {
        if (logUpdateHandler.hasCallbacks(logUpdater)) {
            logUpdateHandler.removeCallbacks(logUpdater);
        }
    }

    private void showNeedSendSms() {
        if (sendSmsCheckBox != null) {
            sendSmsCheckBox.setChecked(appSettings.getNeedSendSms());
        }
    }

    void saveNeedSendSms() {
        if (sendSmsCheckBox == null) {
            return;
        }

        boolean checked = sendSmsCheckBox.isChecked();
        appSettings.saveNeedSendSms(checked);
    }

    static class SmsRequestListener implements SmsApi.RequestHandledListener {
        private final MainActivity activity;

        public SmsRequestListener(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onSuccess(long dbId) {
            Intent intent = new Intent(activity, MainActivity.class);

            intent.putExtra("action", "renewMessages");
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            activity.startActivity(intent);
        }

        @Override
        public void onError(long dbId, String errorMessage) {
            Intent intent = new Intent(activity, MainActivity.class);

            intent.putExtra("action", "toastMessage");
            intent.putExtra("message", errorMessage);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            activity.startActivity(intent);
        }
    }

    static class LogUpdater implements Runnable {
        private final MainActivity activity;

        LogUpdater(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        public void run() {
            activity.showLogsFromLogger();
            activity.logUpdateHandler.postDelayed(this, 2500);
        }
    }

    static class GetMessagesParams {
        MessageStorage messageStorage;

        GetMessagesParams(MessageStorage messageStorage) {
            this.messageStorage = messageStorage;
        }
    }

    static class GetMessagesAction extends AsyncTask<GetMessagesParams, Void, List<MessageContainer>> {
        @Override
        protected List<MessageContainer> doInBackground(GetMessagesParams... params) {
            GetMessagesParams mainParams = AsyncTaskUtil.getParams(params);
            if (mainParams == null) {
                return null;
            }

            return mainParams.messageStorage.getMessagesTail();
        }
    }
}
