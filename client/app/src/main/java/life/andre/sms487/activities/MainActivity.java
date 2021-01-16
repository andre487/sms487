package life.andre.sms487.activities;

import android.app.Activity;
import android.content.Intent;
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
import life.andre.sms487.utils.BgTask;

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

        BgTask.run(this::getSettingsToShow).onSuccess(this::showSettings);

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
                toastShowText(intent.getStringExtra("message"));
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

        MessageCleanupWorker.schedulePeriodic();
        MessageResendWorker.scheduleOneTime();
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
        sendSmsCheckBox.setOnCheckedChangeListener((v, c) -> {
            this.saveNeedSendSms();
        });

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

    private void toastShowText(@Nullable String message) {
        if (message != null) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    @NonNull
    private SettingsToShow getSettingsToShow() {
        SettingsToShow settings = new SettingsToShow();

        settings.serverUrl = appSettings.getServerUrl();
        settings.serverKey = appSettings.getServerKey();
        settings.needSendSms = appSettings.getNeedSendSms();

        return settings;
    }

    private void showSettings(@NonNull SettingsToShow v) {
        showServerUrl(v.serverUrl);
        showServerKey(v.serverKey);
        showNeedSendSms(v.needSendSms);
    }

    public void showServerUrl(@NonNull String serverUrl) {
        if (serverUrlInput == null) {
            return;
        }
        serverUrlInput.setText(serverUrl);
    }

    void saveServerUrl() {
        if (serverUrlInput == null) {
            return;
        }

        BgTask.run(() -> {
            Editable serverUrlText = serverUrlInput.getText();
            if (serverUrlText == null) {
                Logger.w(TAG, "serverUrlText is null");
                return null;
            }
            return appSettings.saveServerUrl(serverUrlText.toString());
        }).onSuccess(this::toastShowText);
    }

    public void showServerKey(@NonNull String serverKey) {
        if (serverKeyInput == null) {
            return;
        }

        serverKeyInput.setText(serverKey);
    }

    void saveServerKey() {
        if (serverKeyInput == null) {
            return;
        }

        BgTask.run(() -> {
            Editable serverKeyText = serverKeyInput.getText();
            if (serverKeyText == null) {
                Logger.w(TAG, "serverKeyText is null");
                return null;
            }
            return appSettings.saveServerKey(serverKeyText.toString());
        }).onSuccess(this::toastShowText);
    }

    private void showNeedSendSms(boolean needSendSms) {
        if (sendSmsCheckBox != null) {
            sendSmsCheckBox.setChecked(needSendSms);
        }
    }

    void saveNeedSendSms() {
        if (sendSmsCheckBox == null) {
            return;
        }

        BgTask.run(() -> {
            boolean checked = sendSmsCheckBox.isChecked();
            return appSettings.saveNeedSendSms(checked);
        }).onSuccess(this::toastShowText);
    }

    @NonNull
    private List<MessageContainer> getMessages() {
        return messageStorage.getMessagesTail();
    }

    private void showMessages() {
        if (messagesField == null) {
            return;
        }
        BgTask.run(this::getMessages).onSuccess(this::setMessagesToField);
    }

    private void setMessagesToField(@NonNull List<MessageContainer> messages) {
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

    private void showLogsFromLogger() {
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

    private static class SmsRequestListener implements ServerApi.RequestHandledListener {
        private final MainActivity activity;

        SmsRequestListener(MainActivity activity) {
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

    private static class LogUpdater implements Runnable {
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

    private static class SettingsToShow {
        @NonNull
        String serverUrl = "";
        @NonNull
        String serverKey = "";
        boolean needSendSms = false;
    }
}
