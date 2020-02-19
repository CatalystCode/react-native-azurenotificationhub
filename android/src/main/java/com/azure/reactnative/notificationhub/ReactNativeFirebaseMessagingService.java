package com.azure.reactnative.notificationhub;

import android.app.NotificationChannel;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class ReactNativeFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "ReactNativeFirebaseMessagingService";

    private static ReactNativeNotificationsHandler notificationHandler;

    public static void createNotificationHandler(Context context) {
        if (notificationHandler == null) {
            notificationHandler = new ReactNativeNotificationsHandler();
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

            NotificationChannel channel = builder.build();
            notificationHandler.createChannelAndHandleNotifications(context, channel);
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
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (notificationHandler == null) {
            Log.e(TAG, "Notification handler hasn't been created");
            return;
        }

        Bundle bundle = remoteMessage.toIntent().getExtras();
        notificationHandler.sendNotification(bundle);
        notificationHandler.sendBroadcast(bundle, 0);
    }
}
