package life.andre.sms487.auth;

import android.webkit.JavascriptInterface;
import androidx.annotation.NonNull;

import java.util.function.Consumer;

public class AuthAppJsInterface {
    @NonNull
    private final Consumer<String> onData;

    public AuthAppJsInterface(@NonNull Consumer<String> onData) {
        this.onData = onData;
    }

    @JavascriptInterface
    public void sendData(String data) {
        this.onData.accept(data);
    }
}
