package com.azure.reactnative.notificationhub;

import android.app.IntentService;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.microsoft.windowsazure.notifications.NotificationsManager;
import com.microsoft.windowsazure.messaging.NotificationHub;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReactNativeRegistrationIntentService extends IntentService {

    public static final String TAG = "ReactNativeRegistration";

    private final ExecutorService pool = Executors.newFixedThreadPool(1);

    public ReactNativeRegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        final Intent event = new Intent(TAG);
        final NotificationHubUtil notificationHubUtil = NotificationHubUtil.getInstance();
        final String connectionString = notificationHubUtil.getConnectionString(this);
        final String hubName = notificationHubUtil.getHubName(this);
        final String storedToken = notificationHubUtil.getFCMToken(this);
        final String[] tags = notificationHubUtil.getTags(this);

        if (connectionString == null || hubName == null) {
            // The intent was triggered when no connection string has been set.
            // This is likely due to an InstanceID refresh occurring while no user
            // registration is active for Azure Notification Hub.
            return;
        }

        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(
                pool, new OnSuccessListener<InstanceIdResult>() {
                    @Override
                    public void onSuccess(InstanceIdResult instanceIdResult) {
                        try {
                            String regID = notificationHubUtil.getRegistrationID(ReactNativeRegistrationIntentService.this);
                            String token = instanceIdResult.getToken();
                            Log.d(TAG, "FCM Registration Token: " + token);

                            // Storing the registration ID indicates whether the generated token has been
                            // sent to your server. If it is not stored, send the token to your server.
                            // Also check if the token has been compromised and needs refreshing.
                            if (regID == null || storedToken != token) {
                                NotificationHub hub = new NotificationHub(hubName, connectionString,
                                        ReactNativeRegistrationIntentService.this);
                                Log.d(TAG, "NH Registration refreshing with token : " + token);

                                regID = hub.register(token, tags).getRegistrationId();
                                Log.d(TAG, "New NH Registration Successfully - RegId : " + regID);

                                notificationHubUtil.setRegistrationID(ReactNativeRegistrationIntentService.this, regID);
                                notificationHubUtil.setFCMToken(ReactNativeRegistrationIntentService.this, token);

                                NotificationsManager.handleNotifications(ReactNativeRegistrationIntentService.this, regID, ReactNativeNotificationsHandler.class);

                                event.putExtra("event", ReactNativeNotificationHubModule.NOTIF_REGISTER_AZURE_HUB_EVENT);
                                event.putExtra("data", regID);
                                localBroadcastManager.sendBroadcast(event);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to complete token refresh", e);

                            event.putExtra("event", ReactNativeNotificationHubModule.NOTIF_AZURE_HUB_REGISTRATION_ERROR_EVENT);
                            event.putExtra("data", e.getMessage());
                            localBroadcastManager.sendBroadcast(event);
                        }
                    }
                });
    }
}
