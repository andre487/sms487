package life.andre.sms487.activities.main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;

import life.andre.sms487.R;
import life.andre.sms487.logging.Logger;
import life.andre.sms487.messageStorage.MessageContainer;
import life.andre.sms487.messageStorage.MessageStorage;
import life.andre.sms487.preferences.AppSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    protected MessageStorage messageStorage;
    protected AppSettings appSettings;

    @Nullable @BindView(R.id.messagesField)
    AppCompatTextView messagesField;

    @Nullable @BindView(R.id.userNameInput)
    AppCompatEditText userNameInput;

    @Nullable @BindView(R.id.serverKeyInput)
    AppCompatEditText serverKeyInput;

    @Nullable @BindView(R.id.serverUrlInput)
    AppCompatEditText serverUrlInput;

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

        requestPermissions();

        showMessagesFromDb();
        showLogsFromLogger();

        showServerUrl();
        showServerKey();
    }

    void requestPermissions() {
        List<String> permList = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            permList.add(Manifest.permission.RECEIVE_SMS);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            permList.add(Manifest.permission.INTERNET);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            permList.add(Manifest.permission.ACCESS_NETWORK_STATE);
        }

        int permCount = permList.size();
        if (permCount > 0) {
            String[] permArr = permList.toArray(new String[permCount]);
            ActivityCompat.requestPermissions(this, permArr, 487);
        }
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

    @OnClick(R.id.serverKeySave)
    void saveServerKey() {
        AppCompatEditText userNameInput = findViewById(R.id.userNameInput);
        if (userNameInput == null) {
            return;
        }

        AppCompatEditText serverKeyInput = findViewById(R.id.serverKeyInput);
        if (serverKeyInput == null) {
            return;
        }

        appSettings.saveUserName(userNameInput.getText().toString());
        appSettings.saveServerKey(serverKeyInput.getText().toString());
    }

    public void showServerUrl() {
        AppCompatEditText serverUrlInput = findViewById(R.id.serverUrlInput);
        if (serverUrlInput == null) {
            return;
        }

        serverUrlInput.setText(appSettings.getServerUrl());
    }

    @OnClick(R.id.serverUrlSave)
    void saveServerUrl() {
        AppCompatEditText serverUrlInput = findViewById(R.id.serverUrlInput);
        if (serverUrlInput == null) {
            return;
        }

        appSettings.saveServerUrl(serverUrlInput.getText().toString());
    }

    public void showServerKey() {
        AppCompatEditText userNameInput = findViewById(R.id.userNameInput);
        if (userNameInput == null) {
            return;
        }

        AppCompatEditText serverKeyInput = findViewById(R.id.serverKeyInput);
        if (serverKeyInput == null) {
            return;
        }

        userNameInput.setText(appSettings.getUserName());
        serverKeyInput.setText(appSettings.getServerKey());
    }

    private ArrayList<MessageContainer> getMessages() {
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
        AppCompatTextView messagesField = findViewById(R.id.messagesField);
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
        AppCompatTextView logsField = findViewById(R.id.logsField);
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
