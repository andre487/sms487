package life.andre.sms487.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
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
    private boolean lockSettingsSave = false;

    private EditText serverKeyInput;
    private EditText authUrlInput;
    private EditText serverUrlInput;
    private CheckBox sendSmsCheckBox;
    private TextView messagesField;
    private TextView logsField;

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
        authUrlInput = findViewById(R.id.authUrlInput);
        serverUrlInput = findViewById(R.id.serverUrlInput);
        serverKeyInput = findViewById(R.id.serverKeyInput);
        sendSmsCheckBox = findViewById(R.id.sendSmsCheckBox);
        messagesField = findViewById(R.id.messagesField);
        logsField = findViewById(R.id.logsField);
    }

    private void bindEvents() {
        authUrlInput.setOnEditorActionListener((v, actionId, event) -> {
            saveAuthUrl();
            return false;
        });
        serverUrlInput.setOnEditorActionListener((v, actionId, event) -> {
            saveServerUrl();
            return false;
        });
        serverKeyInput.setOnEditorActionListener((v, actionId, event) -> {
            saveServerKey();
            return false;
        });
        sendSmsCheckBox.setOnCheckedChangeListener((v, c) -> saveNeedSendSms());
    }

    private void showSettings() {
        BgTask.run(this::getSettingsToShow).onSuccess(this::fillSettingFields);
    }

    @NonNull
    private SettingsToShow getSettingsToShow() {
        AppSettings st = AppSettings.getInstance();
        return new SettingsToShow(
            st.getAuthUrl(),
            st.getServerUrl(),
            st.getServerKey(),
            st.getNeedSendSms()
        );
    }

    private void fillSettingFields(@NonNull SettingsToShow st) {
        lockSettingsSave = true;

        showAuthUrl(st.authUrl);
        showServerUrl(st.serverUrl);
        showServerKey(st.serverKey);
        showNeedSendSms(st.needSendSms);

        lockSettingsSave = false;
    }

    public void showAuthUrl(@NonNull String authUrl) {
        if (authUrlInput != null) {
            authUrlInput.setText(authUrl);
        }
    }

    public void showServerUrl(@NonNull String serverUrl) {
        if (serverUrlInput != null) {
            serverUrlInput.setText(serverUrl);
        }
    }

    public void showServerKey(@NonNull String serverKey) {
        if (serverKeyInput != null) {
            serverKeyInput.setText(serverKey);
        }
    }

    private void showNeedSendSms(boolean needSendSms) {
        if (sendSmsCheckBox != null) {
            sendSmsCheckBox.setChecked(needSendSms);
        }
    }

    void saveAuthUrl() {
        if (authUrlInput == null || lockSettingsSave) {
            return;
        }

        BgTask.run(() -> {
            Editable authUrlText = authUrlInput.getText();
            if (authUrlText != null) {
                AppSettings.getInstance().saveAuthUrl(authUrlText.toString());
            }
            return null;
        });
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
                    .append('\t').append(message.getDateTime())
                    .append("\nSent: ").append(message.isSent() ? "yes" : "no")
                    .append('\n')
                    .append(message.getBody()).append("\n\n");
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
        public static final int DELAY = 1000;

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
        final String authUrl;
        @NonNull
        final String serverUrl;
        @NonNull
        final String serverKey;
        final boolean needSendSms;

        SettingsToShow(@NonNull String authUrl, @NonNull String serverUrl, @NonNull String serverKey, boolean needSendSms) {
            this.authUrl = authUrl;
            this.serverUrl = serverUrl;
            this.serverKey = serverKey;
            this.needSendSms = needSendSms;
        }
    }
}
