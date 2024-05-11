package life.andre.sms487.auth;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.work.*;
import life.andre.sms487.R;
import life.andre.sms487.auth.errors.TokenException;
import life.andre.sms487.logging.Logger;
import life.andre.sms487.network.AuthApi;
import life.andre.sms487.settings.AppSettings;
import life.andre.sms487.utils.ValueOrError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class TokenCheckWorker extends Worker {
    public static final String TAG = "TCW";
    public static final String TASK_ID = "TokenCheckWorker";
    public static final String CHANNEL_ID = "TokenCheckWorker::TokenInfo";

    @Nullable
    private static Bitmap largeIconCache = null;

    public static void schedulePeriodic(@NonNull Context ctx) {
        var task = new PeriodicWorkRequest.Builder(TokenCheckWorker.class, 4, TimeUnit.HOURS).build();
        WorkManager
            .getInstance(ctx)
            .enqueueUniquePeriodicWork(TASK_ID, ExistingPeriodicWorkPolicy.KEEP, task);

        Logger.i(TAG, "Schedule token check");
    }

    public TokenCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @NotNull
    @Override
    public Result doWork() {
        AppSettings settings;
        AuthApi authApi;
        try {
            settings = AppSettings.getInstance();
            authApi = AuthApi.getInstance();
        } catch (NullPointerException e) {
            Logger.e(TAG, "Failed to get global objects: " + e);
            return Result.failure();
        }

        var authUrl = settings.getAuthUrl();
        if (authUrl.isEmpty()) {
            showNotification("Auth URL is empty");
            return Result.success();
        }

        if (settings.getServerUrl().isEmpty()) {
            showNotification("Server URL is empty");
            return Result.success();
        }

        var serverKey = settings.getServerKey();
        if (serverKey.isEmpty()) {
            showNotification("Server token is empty");
            return Result.success();
        }

        var pkFuture = new CompletableFuture<ValueOrError<String, Exception>>();
        authApi.getPublicKey(
            authUrl,
            pk -> pkFuture.complete(new ValueOrError<>(pk)),
            err -> pkFuture.complete(new ValueOrError<>(err))
        );

        ValueOrError<String, Exception> pkOrError;
        try {
            pkOrError = pkFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            Logger.e(TAG, "Failed to get public key: " + e);
            return Result.failure();
        }

        if (!pkOrError.success()) {
            Logger.e(TAG, "Failed to get public key: " + pkOrError.getError());
            return Result.failure();
        }

        var tdOrError = TokenData.create(pkOrError.getValueNonNull(), serverKey);
        if (!tdOrError.success()) {
            handleTokenError(tdOrError.getErrorNonNull());
            return Result.success();
        }

        handleTokenData(tdOrError.getValueNonNull());
        return Result.success();
    }

    private void handleTokenData(@NonNull TokenData tokenData) {
        var issuesMessages = new LinkedList<String>();

        var expirationDiff = tokenData.getExpiresAt().getTime() - new Date().getTime();
        if (expirationDiff < 48 * 60 * 60 * 1000) {
            issuesMessages.add("Token will expire in less then 48 hours.");
        }

        if (!tokenData.getAccess().sms()) {
            issuesMessages.add("Token is not authorized to access to SMS.");
        }

        if (!issuesMessages.isEmpty()) {
            showNotification(String.join("\n", issuesMessages));
        }
    }

    private void handleTokenError(@NonNull TokenException err) {
        var messageBuilder = "Token error: [" + err.getErrorType() + "] " + err.getMessage();
        showNotification(messageBuilder);
    }

    private void showNotification(@NonNull String text) {
        var ctx = this.getApplicationContext();

        var notification = new Notification.Builder(ctx, CHANNEL_ID)
            .setContentTitle("SMS 487: Token issue")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(getLargeIcon(ctx))
            .setContentIntent(getAppOpenIntent(ctx))
            .build();

        postNotification(ctx, notification);
    }

    @NonNull
    private PendingIntent getAppOpenIntent(Context ctx) {
        var intent = new Intent(ctx, this.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    private static void postNotification(@NonNull Context ctx, @NonNull Notification notification) {
        var manager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            Logger.w(TAG, "NotificationManager is null");
            return;
        }

        var channel = new NotificationChannel(
            CHANNEL_ID, "SMS 487 Token issues",
            NotificationManager.IMPORTANCE_HIGH
        );
        manager.createNotificationChannel(channel);

        manager.notify(0, notification);
    }

    @Nullable
    private static Bitmap getLargeIcon(@NonNull Context ctx) {
        if (largeIconCache != null) {
            return largeIconCache;
        }

        var largeVector = ResourcesCompat.getDrawable(ctx.getResources(), R.drawable.baseline_warning_amber_24, null);
        if (largeVector == null) {
            return null;
        }

        var largeIcon = Bitmap.createBitmap(
            largeVector.getIntrinsicWidth(),
            largeVector.getIntrinsicHeight(),
            Bitmap.Config.ARGB_8888
        );

        var canvas = new Canvas(largeIcon);
        largeVector.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        largeVector.draw(canvas);

        largeIconCache = largeIcon;
        return largeIcon;
    }
}
