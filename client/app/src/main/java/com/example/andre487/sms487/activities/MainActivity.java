package com.example.andre487.sms487.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;

import com.example.andre487.sms487.R;
import com.example.andre487.sms487.messages.MessageContainer;
import com.example.andre487.sms487.messages.MessageStorage;
import com.example.andre487.sms487.preferences.AppSettings;
import com.example.andre487.sms487.services.SmsHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    static class GetMessageParams {
        MessageStorage messageStorage;

        GetMessageParams(MessageStorage messageStorage) {
            this.messageStorage = messageStorage;
        }
    }

    static class GetMessageAction extends AsyncTask<GetMessageParams, Void, ArrayList<MessageContainer>> {
        @Override
        protected ArrayList<MessageContainer> doInBackground(GetMessageParams... params) {
            if (params.length == 0) {
                Log.w("GetMessageAction", "Params length is 0");
                return null;
            }

            GetMessageParams mainParams = params[0];

            return mainParams.messageStorage.getMessagesTail();
        }
    }

    static class NewSmsHandler extends Handler {
        MainActivity activity;

        NewSmsHandler(MainActivity activity) {
            super();
            this.activity = activity;
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d("MainActivity", "Got incoming SMS: " + msg.toString());
            // TODO: Implement auto refresh
        }

        void destroy() {
            activity = null;
        }
    }

    protected NewSmsHandler incomingSmsHandler = new NewSmsHandler(this);

    protected ServiceConnection smsHandlerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            SmsHandler.SmsBridge binder = (SmsHandler.SmsBridge) service;
            smsHandler = binder.getService();
            smsHandler.setNewSmsHandler(MainActivity.this, incomingSmsHandler);
            smsHandlerBound = true;

            Log.d("MainActivity", "SmsHandler bound");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            smsHandlerBound = false;
            smsHandler.removeNewSmsHandler(MainActivity.this);

            Log.d("MainActivity", "SmsHandler unbound");
        }
    };

    protected MessageStorage messageStorage;
    protected SmsHandler smsHandler;
    protected boolean smsHandlerBound = false;

    protected AppSettings appSettings;

    @Nullable @BindView(R.id.messagesField)
    AppCompatTextView messagesField;

    @Nullable @BindView(R.id.serverKeyInput)
    AppCompatEditText serverKeyInput;

    @Nullable @BindView(R.id.serverUrlInput)
    AppCompatEditText serverUrlInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Log.d("MainActivity", "Activity is started");

        messageStorage = new MessageStorage(this);
        appSettings = new AppSettings(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        showMessagesFromDb();

        Intent intent = new Intent(this, SmsHandler.class);
        bindService(intent, smsHandlerConnection, Context.BIND_AUTO_CREATE);

        showServerUrl();
        showServerKey();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (smsHandlerBound) {
            unbindService(smsHandlerConnection);
            smsHandlerBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        incomingSmsHandler.destroy();
    }

    @OnClick(R.id.renewMessages)
    void showMessagesFromDb() {
        final List<MessageContainer> messages = getMessages();
        if (messages == null) {
            Log.w("MainActivity", "Messages are null");
            return;
        }
        Log.w("MainActivity", "show messages");
        showMessages(messages);
    }

    @OnClick(R.id.serverKeySave)
    void saveServerKey() {
        AppCompatEditText serverKeyInput = findViewById(R.id.serverKeyInput);
        if (serverKeyInput == null) {
            return;
        }
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
        AppCompatEditText serverKeyInput = findViewById(R.id.serverKeyInput);
        if (serverKeyInput == null) {
            return;
        }

        serverKeyInput.setText(appSettings.getServerKey());
    }

    private ArrayList<MessageContainer> getMessages() {
        GetMessageParams params = new GetMessageParams(messageStorage);
        GetMessageAction action = new GetMessageAction();
        action.execute(params);

        try {
            return action.get();
        } catch (InterruptedException | ExecutionException e) {
            Log.w("MainActivity", "Get messages error: " + e.toString());
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
}
