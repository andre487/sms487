package life.andre.sms487.network;

import android.content.Context;
import androidx.annotation.NonNull;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.Objects;
import java.util.function.Consumer;

public class AuthApi {
    public static final String TAG = "AAPI";

    private static AuthApi instance;

    public static void init(@NonNull Context ctx) {
        instance = new AuthApi(ctx);
    }

    @NonNull
    public static AuthApi getInstance() {
        return Objects.requireNonNull(instance, "Not initialized");
    }

    @NonNull
    private final RequestQueue requestQueue;

    private AuthApi(@NonNull Context ctx) {
        requestQueue = Volley.newRequestQueue(ctx);
    }

    public void getPublicKey(
        @NonNull String authUrl,
        @NonNull Consumer<String> onSuccess,
        @NonNull Consumer<Exception> onError
    ) {
        var req = new StringRequest(
            Request.Method.GET,
            authUrl + "/get-public-key",
            onSuccess::accept,
            onError::accept
        )
            .setRetryPolicy(new DefaultRetryPolicy(1000, 10, 2))
            .setShouldRetryConnectionErrors(true)
            .setShouldRetryServerErrors(true);

        requestQueue.add(req);
    }
}
