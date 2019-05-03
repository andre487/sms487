package life.andre.sms487.activities.main;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.util.Log;

import life.andre.sms487.R;
import life.andre.sms487.logging.Logger;
import life.andre.sms487.messages.MessageContainer;
import life.andre.sms487.messages.MessageStorage;
import life.andre.sms487.preferences.AppSettings;

import java.util.List;
import java.util.concurrent.ExecutionException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import life.andre.sms487.system.PermissionsChecker;

public class MainActivity extends AppCompatActivity {
    private PermissionsChecker permissionsChecker = new PermissionsChecker(this);

    private MessageStorage messageStorage;
    private AppSettings appSettings;

    @Nullable @BindView(R.id.serverKeyInput)
    AppCompatEditText serverKeyInput;

    @Nullable @BindView(R.id.serverUrlInput)
    AppCompatEditText serverUrlInput;

    @Nullable @BindView(R.id.messagesField)
    AppCompatEditText messagesField;

    @Nullable @BindView(R.id.logsField)
    AppCompatEditText logsField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Logger.d("MainActivity", "Activity is started");

        messageStorage = new MessageStorage(this);
        appSettings = new AppSettings(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        permissionsChecker.checkPermissions();

        showMessagesFromDb();
        showLogsFromLogger();

        showServerUrl();
        showServerKey();
    }

    @OnClick(R.id.renewMessages)
    void showMessagesFromDb() {
        final List<MessageContainer> messages = getMessages();
        if (messages == null) {
            Logger.w("MainActivity", "Messages are null");
            return;
        }
        Logger.w("MainActivity", "show messages");
        showMessages(messages);
    }

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

    @SuppressWarnings("unused")
    @OnClick(R.id.serverKeySave)
    void saveServerKey() {
        if (serverKeyInput == null) {
            return;
        }

        appSettings.saveServerKey(serverKeyInput.getText().toString());
    }

    public void showServerUrl() {
        if (serverUrlInput == null) {
            return;
        }

        serverUrlInput.setText(appSettings.getServerUrl());
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.serverUrlSave)
    void saveServerUrl() {
        if (serverUrlInput == null) {
            return;
        }

        appSettings.saveServerUrl(serverUrlInput.getText().toString());
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
