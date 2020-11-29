package life.andre.sms487.activities.main;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;

import android.text.Editable;
import android.util.Log;

import life.andre.sms487.R;
import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageContainer;
import life.andre.sms487.messages.MessageStorage;
import life.andre.sms487.network.SmsApi;
import life.andre.sms487.preferences.AppSettings;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import life.andre.sms487.services.smsHandler.SmsRequestListener;
import life.andre.sms487.system.PermissionsChecker;

public class MainActivity extends AppCompatActivity {
    private final PermissionsChecker permissionsChecker = new PermissionsChecker(this);

    private MessageStorage messageStorage;
    private SmsApi smsApi;
    private AppSettings appSettings;

    @SuppressLint("NonConstantResourceId")
    @Nullable
    @BindView(R.id.serverKeyInput)
    AppCompatEditText serverKeyInput;

    @SuppressLint("NonConstantResourceId")
    @Nullable
    @BindView(R.id.serverUrlInput)
    AppCompatEditText serverUrlInput;

    @SuppressLint("NonConstantResourceId")
    @Nullable
    @BindView(R.id.messagesField)
    AppCompatEditText messagesField;

    @SuppressLint("NonConstantResourceId")
    @Nullable
    @BindView(R.id.logsField)
    AppCompatEditText logsField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Logger.d("MainActivity", "Activity is started");

        messageStorage = new MessageStorage(this);
        appSettings = new AppSettings(this);
        smsApi = new SmsApi(this, appSettings);

        SmsRequestListener smsRequestListener = new SmsRequestListener(
                messageStorage,
                "MainActivity"
        );
        smsApi.addRequestHandledListener(smsRequestListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        permissionsChecker.checkPermissions();

        renewMessagesFromDb();
        showLogsFromLogger();

        showServerUrl();
        showServerKey();
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.renewMessages)
    void renewMessagesFromDb() {
        resendMessages();

        final List<MessageContainer> messages = getMessages();
        if (messages == null) {
            Logger.w("MainActivity", "Messages are null");
            return;
        }
        Logger.w("MainActivity", "show messages");
        showMessages(messages);
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.renewLogs)
    void showLogsFromLogger() {
        final List<String> logs = getLogs();
        if (logs == null) {
            Log.w("MainActivity", "Messages are null");
            return;
        }
        Log.i("MainActivity", "Show logs");
        showLogs(logs);
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.serverKeySave)
    void saveServerKey() {
        if (serverKeyInput == null) {
            return;
        }

        Editable serverKeyText = Objects.requireNonNull(serverKeyInput.getText());
        appSettings.saveServerKey(serverKeyText.toString());
    }

    public void showServerUrl() {
        if (serverUrlInput == null) {
            return;
        }

        serverUrlInput.setText(appSettings.getServerUrl());
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.serverUrlSave)
    void saveServerUrl() {
        if (serverUrlInput == null) {
            return;
        }

        Editable serverUrlText = Objects.requireNonNull(serverUrlInput.getText());
        appSettings.saveServerUrl(serverUrlText.toString());
    }

    public void showServerKey() {
        if (serverKeyInput == null) {
            return;
        }

        serverKeyInput.setText(appSettings.getServerKey());
    }

    private void resendMessages() {
        ResendMessagesParams params = new ResendMessagesParams(messageStorage, smsApi);
        ResendMessagesAction action = new ResendMessagesAction();
        action.execute(params);

        try {
            action.get();
        } catch (InterruptedException | ExecutionException e) {
            Logger.w("MainActivity", "Resent messages error: " + e.toString());
            e.printStackTrace();
        }
    }

    private List<MessageContainer> getMessages() {
        GetMessagesParams params = new GetMessagesParams(messageStorage);
        GetMessagesAction action = new GetMessagesAction();
        action.execute(params);

        try {
            return action.get();
        } catch (InterruptedException | ExecutionException e) {
            Logger.w("MainActivity", "Get messages error: " + e.toString());
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

        messagesField.setText(messagesString.toString());
    }

    private List<String> getLogs() {
        GetLogsAction action = new GetLogsAction();
        action.execute();

        try {
            return action.get();
        } catch (InterruptedException | ExecutionException e) {
            Log.w("MainActivity", "Get logs error: " + e.toString());
            e.printStackTrace();
            return null;
        }
    }

    private void showLogs(List<String> logs) {
        if (logsField == null) {
            return;
        }

        StringBuilder logsString = new StringBuilder();

        for (String logLine : logs) {
            logsString.append(logLine);
            logsString.append('\n');
        }

        logsField.setText(logsString.toString());
    }
}
