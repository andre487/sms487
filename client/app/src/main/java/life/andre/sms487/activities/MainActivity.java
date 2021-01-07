package life.andre.sms487.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.ExecutionException;

import life.andre.sms487.R;
import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageCleanupWorker;
import life.andre.sms487.messages.MessageContainer;
import life.andre.sms487.messages.MessageResendWorker;
import life.andre.sms487.messages.MessageStorage;
import life.andre.sms487.network.ServerApi;
import life.andre.sms487.services.NotificationListener;
import life.andre.sms487.settings.AppSettings;
import life.andre.sms487.system.PermissionsChecker;
import life.andre.sms487.utils.AsyncTaskUtil;

public class MainActivity extends Activity {
    private final static String TAG = "MainActivity";

    private final PermissionsChecker permissionsChecker = new PermissionsChecker(this);
    private final Handler logUpdateHandler = new Handler();
    private final LogUpdater logUpdater = new LogUpdater(this);
    private final ServerApi.RequestHandledListener smsRequestListener = new SmsRequestListener(this);

    private MessageStorage messageStorage;
    private ServerApi serverApi;
    private AppSettings appSettings;

    private EditText serverKeyInput;
    private EditText serverUrlInput;
    private TextView messagesField;
    private TextView logsField;

    private Button saveServerKeyButton;
    private Button saveServerUrlButton;

    private CheckBox sendSmsCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createFieldValues();
        startServiceTasks();
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
        showMessages();
        enableLogAutoRenew();

        serverApi.addRequestHandledListener(smsRequestListener);
        serverApi.resendMessages();
    }

    @Override
    protected void onStop() {
        serverApi.removeRequestHandledListener(smsRequestListener);
        disableLogAutoRenew();
        super.onStop();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);

        String action = intent.getStringExtra("action");
        if (action == null) {
            return;
        }

        switch (action) {
            case "renewMessages":
                showMessages();
                break;
            case "toastMessage":
                String message = intent.getStringExtra("message");
                if (message != null) {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private void createFieldValues() {
        messageStorage = new MessageStorage(this);
        appSettings = new AppSettings(this);
        serverApi = new ServerApi(this);
    }

    private void startServiceTasks() {
        Intent intent = new Intent(this, NotificationListener.class);
        startService(intent);

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
        saveServerUrlButton.setOnClickListener(v -> this.saveServerUrl());
        saveServerKeyButton.setOnClickListener(v -> this.saveServerKey());
        sendSmsCheckBox.setOnCheckedChangeListener((v, c) -> this.saveNeedSendSms());

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

    void saveServerKey() {
        if (serverKeyInput == null) {
            return;
        }

        Editable serverKeyText = serverKeyInput.getText();
        if (serverKeyText != null) {
            appSettings.saveServerKey(serverKeyText.toString());
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

    void showMessages() {
        if (messagesField == null) {
            return;
        }

        final List<MessageContainer> messages = getMessages();
        if (messages == null) {
            Logger.w(TAG, "Messages are null");
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

    @Nullable
    private List<MessageContainer> getMessages() {
        GetMessagesParams params = new GetMessagesParams(messageStorage);
        GetMessagesAction action = new GetMessagesAction();
        action.execute(params);

        try {
            return action.get();
        } catch (@NonNull InterruptedException | ExecutionException e) {
            Logger.w(TAG, "Get messages error: " + e.toString());
            e.printStackTrace();
            return null;
        }
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

    private void enableLogAutoRenew() {
        logUpdateHandler.post(logUpdater);
    }

    private void disableLogAutoRenew() {
        logUpdateHandler.removeCallbacks(logUpdater);
    }

    static class SmsRequestListener implements ServerApi.RequestHandledListener {
        private final MainActivity activity;

        public SmsRequestListener(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onSuccess() {
            Intent intent = new Intent(activity, MainActivity.class);

            intent.putExtra("action", "renewMessages");
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            activity.startActivity(intent);
        }

        @Override
        public void onError(String errorMessage) {
            Intent errorIntent = new Intent(activity, MainActivity.class);
            errorIntent.putExtra("action", "toastMessage");
            errorIntent.putExtra("message", errorMessage);
            errorIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(errorIntent);

            Intent renewIntent = new Intent(activity, MainActivity.class);
            renewIntent.putExtra("action", "renewMessages");
            renewIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(renewIntent);
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
        final MessageStorage messageStorage;

        GetMessagesParams(MessageStorage messageStorage) {
            this.messageStorage = messageStorage;
        }
    }

    static class GetMessagesAction extends AsyncTask<GetMessagesParams, Void, List<MessageContainer>> {
        @Nullable
        @Override
        protected List<MessageContainer> doInBackground(@NonNull GetMessagesParams... params) {
            GetMessagesParams mainParams = AsyncTaskUtil.getParams(params);
            if (mainParams == null) {
                return null;
            }

            return mainParams.messageStorage.getMessagesTail();
        }
    }
}
