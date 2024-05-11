package life.andre.sms487.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.Nullable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import life.andre.sms487.R;
import life.andre.sms487.auth.AuthAppJsInterface;
import life.andre.sms487.logging.Logger;
import life.andre.sms487.settings.AppSettings;
import life.andre.sms487.utils.BgTask;
import life.andre.sms487.views.Toaster;

public class AuthActivity extends Activity {
    private static final String TAG = "AA";

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private WebView authWebView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        setupWebView();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        authWebView = findViewById(R.id.webView);
        authWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }
        });
        authWebView.clearCache(true);
        authWebView.clearHistory();

        var cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(null);
        cookieManager.flush();

        var wvSettings = authWebView.getSettings();
        wvSettings.setJavaScriptEnabled(true);

        authWebView.addJavascriptInterface(new AuthAppJsInterface(this::onWebData), "App487Bridge");
    }

    @Override
    protected void onStart() {
        super.onStart();
        BgTask.run(() -> AppSettings.getInstance().getAuthUrl()).onSuccess(this::showAuthView);
    }

    private void showAuthView(@Nullable String authUrl) {
        if (authUrl == null || authUrl.isEmpty()) {
            authWebView.loadUrl("data:text/html,<h1>Auth URL is empty</h1>");
            return;
        }
        authWebView.loadUrl(authUrl);
    }

    private void onWebData(@Nullable String data) {
        BgTask.run(() -> {
            JsonNode dataNode;
            try {
                dataNode = jsonMapper.readTree(data);
            } catch (JsonProcessingException e) {
                Toaster.getInstance().show("Auth error: " + e);
                return false;
            }

            var type = dataNode.get("type").asText();
            if (type == null || "newToken".compareTo(type) != 0) {
                Logger.w(TAG, "Unknown message: " + type);
                return false;
            }

            var tokenContent = dataNode.get("data").asText();
            if (tokenContent == null) {
                Toaster.getInstance().show("Auth error: token is null");
                return false;
            }

            AppSettings.getInstance().saveServerKey(tokenContent);
            return true;
        }).onSuccess((finished) -> {
            if (finished) {
                this.finish();
            }
        });
    }
}
