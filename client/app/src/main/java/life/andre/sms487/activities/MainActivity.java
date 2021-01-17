package life.andre.sms487.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import life.andre.sms487.R;
import life.andre.sms487.events.MessagesStateChanged;
import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageContainer;
import life.andre.sms487.messages.MessageStorage;
import life.andre.sms487.settings.AppSettings;
import life.andre.sms487.system.PermissionsChecker;
import life.andre.sms487.utils.BgTask;

public class MainActivity extends Activity {
    private final LogUpdater logUpdater = new LogUpdater(this::showLogsFromLogger);
    private final EventBus eventBus = EventBus.getDefault();

    private EditText serverKeyInput;
    private EditText serverUrlInput;
    private TextView messagesField;
    private TextView logsField;

    private Button saveServerKeyButton;
    private Button saveServerUrlButton;

    private CheckBox sendSmsCheckBox;

    private boolean lockSettingsSave = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PermissionsChecker.check(this);

        findViewComponents();
        bindEvents();
    }

    @Override
    protected void onStart() {
        super.onStart();
        showSettings();
        showMessages();
        logUpdater.run();
        eventBus.register(this);
    }

    @Override
    protected void onStop() {
        logUpdater.disable();
        eventBus.unregister(this);
        super.onStop();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessagesStateChanged(MessagesStateChanged event) {
        showMessages();
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
        saveServerUrlButton.setOnClickListener(v -> saveServerUrl());
        saveServerKeyButton.setOnClickListener(v -> saveServerKey());
        sendSmsCheckBox.setOnCheckedChangeListener((v, c) -> saveNeedSendSms());

        messagesField.setMovementMethod(new ScrollingMovementMethod());
        logsField.setMovementMethod(new ScrollingMovementMethod());
    }

    private void showSettings() {
        BgTask.run(this::getSettingsToShow).onSuccess(this::fillSettingFields);
    }

    @NonNull
    private SettingsToShow getSettingsToShow() {
        AppSettings appSettings = AppSettings.getInstance();
        SettingsToShow settings = new SettingsToShow();

        settings.serverUrl = appSettings.getServerUrl();
        settings.serverKey = appSettings.getServerKey();
        settings.needSendSms = appSettings.getNeedSendSms();

        return settings;
    }

    private void fillSettingFields(@NonNull SettingsToShow v) {
        lockSettingsSave = true;

        showServerUrl(v.serverUrl);
        showServerKey(v.serverKey);
        showNeedSendSms(v.needSendSms);

        lockSettingsSave = false;
    }

    public void showServerUrl(@NonNull String serverUrl) {
        if (serverUrlInput == null) {
            return;
        }
        serverUrlInput.setText(serverUrl);
    }

    public void showServerKey(@NonNull String serverKey) {
        if (serverKeyInput == null) {
            return;
        }
        serverKeyInput.setText(serverKey);
    }

    private void showNeedSendSms(boolean needSendSms) {
        if (sendSmsCheckBox != null) {
            sendSmsCheckBox.setChecked(needSendSms);
        }
    }

    void saveServerUrl() {
        if (serverUrlInput == null || lockSettingsSave) {
            return;
        }

        BgTask.run(() -> {
            Editable serverUrlText = serverUrlInput.getText();
            if (serverUrlText != null) {
                AppSettings.getInstance().saveServerUrl(serverUrlText.toString());
            }
            return null;
        });
    }

    void saveServerKey() {
        if (serverKeyInput == null || lockSettingsSave) {
            return;
        }

        BgTask.run(() -> {
            Editable serverKeyText = serverKeyInput.getText();
            if (serverKeyText != null) {
                AppSettings.getInstance().saveServerKey(serverKeyText.toString());
            }
            return null;
        });
    }

    void saveNeedSendSms() {
        if (sendSmsCheckBox == null || lockSettingsSave) {
            return;
        }

        BgTask.run(() -> {
            AppSettings.getInstance().saveNeedSendSms(sendSmsCheckBox.isChecked());
            return null;
        });
    }

    private void showMessages() {
        if (messagesField == null) {
            return;
        }
        BgTask.run(this::getMessages).onSuccess(this::setMessagesToField);
    }

    @NonNull
    private List<MessageContainer> getMessages() {
        return MessageStorage.getInstance().getMessagesTail();
    }

    private void setMessagesToField(@NonNull List<MessageContainer> messages) {
        StringBuilder msgVal = new StringBuilder();
        for (MessageContainer message : messages) {
            msgVal.append(message.getAddressFrom())
                    .append('\t')
                    .append(message.getDateTime())
                    .append("\nSent: ")
                    .append(message.isSent() ? "yes" : "no")
                    .append('\n')
                    .append(message.getBody())
                    .append("\n\n");
        }
        messagesField.setText(msgVal.toString().trim());
    }

    private void showLogsFromLogger() {
        if (logsField == null) {
            return;
        }

        StringBuilder logs = new StringBuilder();
        for (String logLine : Logger.getMessages()) {
            logs.append(logLine).append('\n');
        }
        logsField.setText(logs.toString().trim());
    }

    private static class LogUpdater implements Runnable {
        public static final int DELAY = 2500;

        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Runnable action;

        public LogUpdater(Runnable action) {
            this.action = action;
        }

        @Override
        public synchronized void run() {
            action.run();
            handler.postDelayed(this, DELAY);
        }

        public synchronized void disable() {
            handler.removeCallbacks(this);
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
