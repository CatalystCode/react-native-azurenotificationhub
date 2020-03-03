package com.azure.reactnative.notificationhub;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class ReactNativeFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "ReactNativeFMS";

    private static String notificationChannelID;

    public static void createNotificationChannel(Context context) {
        if (notificationChannelID == null) {
            NotificationHubUtil notificationHubUtil = NotificationHubUtil.getInstance();
            NotificationChannelBuilder builder = new NotificationChannelBuilder();
            if (notificationHubUtil.hasChannelImportance(context)) {
                builder.setImportance(notificationHubUtil.getChannelImportance(context));
            }

            if (notificationHubUtil.hasChannelShowBadge(context)) {
                builder.setShowBadge(notificationHubUtil.getChannelShowBadge(context));
            }

            if (notificationHubUtil.hasChannelEnableLights(context)) {
                builder.enableLights(notificationHubUtil.getChannelEnableLights(context));
            }

            if (notificationHubUtil.hasChannelEnableVibration(context)) {
                builder.enableVibration(notificationHubUtil.getChannelEnableVibration(context));
            }

            notificationChannelID = ReactNativeNotificationsHandler.NOTIFICATION_CHANNEL_ID;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = builder.build();
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
                notificationManager.createNotificationChannel(channel);
                notificationChannelID = channel.getId();
            }
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.i(TAG, "Refreshing FCM Registration Token");

        Intent intent = new Intent(this, ReactNativeRegistrationIntentService.class);
        startService(intent);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        NotificationHubUtil notificationHubUtil = NotificationHubUtil.getInstance();
        Log.d(TAG, "Remote message from: " + remoteMessage.getFrom());

        if (notificationChannelID == null) {
            createNotificationChannel(this);
        }

        Bundle bundle = remoteMessage.toIntent().getExtras();
        if (notificationHubUtil.getAppIsForeground()) {
            bundle.putBoolean("foreground", true);
            bundle.putBoolean("userInteraction", false);
            bundle.putBoolean("coldstart", false);
        } else {
            ReactNativeNotificationsHandler.sendNotification(this, bundle, notificationChannelID);
        }
        ReactNativeNotificationsHandler.sendBroadcast(this, bundle, 0);
    }
}
