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

import static com.azure.reactnative.notificationhub.ReactNativeConstants.*;

public class ReactNativeFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "ReactNativeFMS";

    private static String notificationChannelID1;
    private static String notificationChannelID2;

    private static ReactNativeNotificationChannelBuilder createNotificationBuilder(Context context, String id, String name) {
        ReactNativeNotificationHubUtil notificationHubUtil = ReactNativeNotificationHubUtil.getInstance();
        ReactNativeNotificationChannelBuilder builder = ReactNativeNotificationChannelBuilder.Factory.create();

        builder.setID(id);
        builder.setName(name);

        if (notificationHubUtil.hasChannelDescription(context)) {
            builder.setDescription(notificationHubUtil.getChannelDescription(context));
        }

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

        return builder;
    }

    public static void createNotificationChannel(Context context) {
        if (notificationChannelID1 == null && notificationChannelID2 == null) {
            ReactNativeNotificationChannelBuilder builder1 = createNotificationBuilder(context, "channel_default", "Meldingen");
            ReactNativeNotificationChannelBuilder builder2 = createNotificationBuilder(context, "channel_goaltune", "Goal Tune");

            notificationChannelID1 = "channel_default";
            notificationChannelID2 = "channel_goaltune";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel1 = builder1.build();
                NotificationChannel channel2 = builder2.build();

                NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel1);
                    notificationManager.createNotificationChannel(channel2);
                    notificationChannelID1 = channel1.getId();
                    notificationChannelID2 = channel2.getId();
                }
            }
        }
    }

    public static void deleteNotificationChannel(Context context) {
        if (notificationChannelID1 != null || notificationChannelID2 != null) {
            final String channelToDeleteID1 = notificationChannelID1;
            final String channelToDeleteID2 = notificationChannelID2;
            notificationChannelID1 = null;
            notificationChannelID2 = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
                notificationManager.deleteNotificationChannel(channelToDeleteID1);
                notificationManager.deleteNotificationChannel(channelToDeleteID2);
            }
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.i(TAG, "Refreshing FCM Registration Token");

        Intent intent = ReactNativeNotificationHubUtil.IntentFactory.createIntent(this, ReactNativeRegistrationIntentService.class);
        ReactNativeRegistrationIntentService.enqueueWork(this, intent);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        ReactNativeNotificationHubUtil notificationHubUtil = ReactNativeNotificationHubUtil.getInstance();
        Log.d(TAG, "Remote message from: " + remoteMessage.getFrom());

        if (notificationChannelID1 == null && notificationChannelID2 == null) {
            createNotificationChannel(this);
        }

        Bundle bundle = remoteMessage.toIntent().getExtras();
        Log.d("ReactNative", bundle.toString());
        if (notificationHubUtil.getAppIsForeground()) {
            bundle.putBoolean(KEY_REMOTE_NOTIFICATION_FOREGROUND, true);
            bundle.putBoolean(KEY_REMOTE_NOTIFICATION_USER_INTERACTION, false);
            bundle.putBoolean(KEY_REMOTE_NOTIFICATION_COLDSTART, false);
        } else {
            ReactNativeNotificationsHandler.sendNotification(this, bundle, notificationChannelID1);
            ReactNativeNotificationsHandler.sendNotification(this, bundle, notificationChannelID2);
        }

        ReactNativeNotificationsHandler.sendBroadcast(this, bundle, 0);
    }
}
