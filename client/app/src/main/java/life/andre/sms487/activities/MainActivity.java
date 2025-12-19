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

    private EditText serverUserInput;
    private EditText serverPasswordInput;
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

    /** @noinspection unused*/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessagesStateChanged(MessagesStateChanged event) {
        showMessages();
    }

    private void findViewComponents() {
        serverUrlInput = findViewById(R.id.serverUrlInput);
        serverUserInput = findViewById(R.id.serverUserInput);
        serverPasswordInput = findViewById(R.id.serverPasswordInput);
        sendSmsCheckBox = findViewById(R.id.sendSmsCheckBox);
        messagesField = findViewById(R.id.messagesField);
        logsField = findViewById(R.id.logsField);
    }

    private void bindEvents() {
        serverUrlInput.setOnEditorActionListener((v, actionId, event) -> {
            saveServerUrl();
            return false;
        });
        serverUserInput.setOnEditorActionListener((v, actionId, event) -> {
            saveServerUser();
            return false;
        });
        serverPasswordInput.setOnEditorActionListener((v, actionId, event) -> {
            saveServerPassword();
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
            st.getServerUrl(),
            st.getServerUser(),
            st.getServerKey(),
            st.getNeedSendSms()
        );
    }

    private void fillSettingFields(@NonNull SettingsToShow st) {
        lockSettingsSave = true;

        showServerUrl(st.serverUrl);
        showServerUser(st.serverUser);
        showServerPassword(st.serverKey);
        showNeedSendSms(st.needSendSms);

        lockSettingsSave = false;
    }

    public void showServerUrl(@NonNull String serverUrl) {
        if (serverUrlInput != null) {
            serverUrlInput.setText(serverUrl);
        }
    }

    public void showServerUser(@NonNull String serverUser) {
        if (serverUserInput != null) {
            serverUserInput.setText(serverUser);
        }
    }

    public void showServerPassword(@NonNull String serverKey) {
        if (serverPasswordInput != null) {
            serverPasswordInput.setText(serverKey);
        }
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

    void saveServerUser() {
        if (serverUserInput == null || lockSettingsSave) {
            return;
        }

        BgTask.run(() -> {
            Editable serverUserText = serverUserInput.getText();
            if (serverUserText != null) {
                AppSettings.getInstance().saveServerUser(serverUserText.toString());
            }
            return null;
        });
    }

    void saveServerPassword() {
        if (serverPasswordInput == null || lockSettingsSave) {
            return;
        }

        BgTask.run(() -> {
            Editable serverPasswordText = serverPasswordInput.getText();
            if (serverPasswordText != null) {
                AppSettings.getInstance().saveServerKey(serverPasswordText.toString());
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
        final String serverUrl;
        @NonNull
        final String serverUser;
        @NonNull
        final String serverKey;
        final boolean needSendSms;

        SettingsToShow(@NonNull String serverUrl, @NonNull String serverUser, @NonNull String serverKey, boolean needSendSms) {
            this.serverUrl = serverUrl;
            this.serverUser = serverUser;
            this.serverKey = serverKey;
            this.needSendSms = needSendSms;
        }
    }
}
