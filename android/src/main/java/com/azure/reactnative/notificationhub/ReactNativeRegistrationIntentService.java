package com.azure.reactnative.notificationhub;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.microsoft.windowsazure.messaging.NotificationHub;

public class ReactNativeRegistrationIntentService extends IntentService {

    private static final String TAG = "AzureNotificationHub";

    public ReactNativeRegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            NotificationHubUtil notificationHubUtil = NotificationHubUtil.getInstance();
            String connectionString = notificationHubUtil.getConnectionString(this);
            String hubName = notificationHubUtil.getHubName(this);
            String regID = notificationHubUtil.getRegistrationID(this);
            String storedToken = notificationHubUtil.getFCMToken(this);
            if (connectionString == null || hubName == null) {
                // The intent was triggered when no connection string has been set.
                // This is likely due to an InstanceID refresh occurring while no user
                // registration is active for Azure Notification Hub.
                return;
            }

            String token = FirebaseInstanceId.getInstance().getToken();
            Log.d(TAG, "FCM Registration Token: " + token);

            // Storing the registration ID indicates whether the generated token has been
            // sent to your server. If it is not stored, send the token to your server.
            // Also check if the token has been compromised and needs refreshing.
            if (regID == null || storedToken != token) {
                NotificationHub hub = new NotificationHub(hubName, connectionString, this);
                Log.d(TAG, "NH Registration refreshing with token : " + token);
                regID = hub.register(token).getRegistrationId();

                Log.d(TAG, "New NH Registration Successfully - RegId : " + regID);

                notificationHubUtil.setRegistrationID(this, regID);
                notificationHubUtil.setFCMToken(this, token);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to complete token refresh", e);
        }
    }
}
