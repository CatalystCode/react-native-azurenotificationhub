package com.azure.reactnative.notificationhub;

import android.content.Context;
import android.content.Intent;

import androidx.core.app.JobIntentService;

import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.microsoft.windowsazure.messaging.NotificationHub;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReactNativeRegistrationIntentService extends JobIntentService {

    public static final String TAG = "ReactNativeRegistration";

    private static final int JOB_ID = 1000;

    private final ExecutorService mPool = Executors.newFixedThreadPool(1);

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, ReactNativeRegistrationIntentService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(Intent intent) {
        final Intent event = ReactNativeNotificationHubUtil.IntentFactory.createIntent(TAG);
        final ReactNativeNotificationHubUtil notificationHubUtil = ReactNativeNotificationHubUtil.getInstance();
        final String connectionString = notificationHubUtil.getConnectionString(this);
        final String hubName = notificationHubUtil.getHubName(this);
        final String storedToken = notificationHubUtil.getFCMToken(this);
        final String[] tags = notificationHubUtil.getTags(this);
        final boolean isTemplated = notificationHubUtil.isTemplated(this);
        final String templateName = notificationHubUtil.getTemplateName(this);
        final String template = notificationHubUtil.getTemplate(this);

        if (connectionString == null || hubName == null) {
            // The intent was triggered when no connection string has been set.
            // This is likely due to an InstanceID refresh occurring while no user
            // registration is active for Azure Notification Hub.
            return;
        }

        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(
                mPool, new OnSuccessListener<InstanceIdResult>() {
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
                                NotificationHub hub = ReactNativeUtil.createNotificationHub(hubName, connectionString,
                                        ReactNativeRegistrationIntentService.this);
                                Log.d(TAG, "NH Registration refreshing with token : " + token);

                                if (isTemplated) {
                                    regID = hub.registerTemplate(
                                            token, templateName, template, tags).getRegistrationId();
                                } else {
                                    regID = hub.register(token, tags).getRegistrationId();
                                }

                                Log.d(TAG, "New NH Registration Successfully - RegId : " + regID);

                                notificationHubUtil.setRegistrationID(ReactNativeRegistrationIntentService.this, regID);
                                notificationHubUtil.setFCMToken(ReactNativeRegistrationIntentService.this, token);

                                event.putExtra(
                                        ReactNativeConstants.KEY_INTENT_EVENT_NAME,
                                        ReactNativeConstants.EVENT_AZURE_NOTIFICATION_HUB_REGISTERED);
                                event.putExtra(
                                        ReactNativeConstants.KEY_INTENT_EVENT_TYPE,
                                        ReactNativeConstants.INTENT_EVENT_TYPE_STRING);
                                event.putExtra(
                                        ReactNativeConstants.KEY_INTENT_EVENT_STRING_DATA, regID);
                                ReactNativeNotificationsHandler.sendBroadcast(
                                        ReactNativeRegistrationIntentService.this, event, 0);

                                // Create notification handler
                                ReactNativeFirebaseMessagingService.createNotificationChannel(
                                        ReactNativeRegistrationIntentService.this);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to complete token refresh", e);

                            event.putExtra(
                                    ReactNativeConstants.KEY_INTENT_EVENT_NAME,
                                    ReactNativeConstants.EVENT_AZURE_NOTIFICATION_HUB_REGISTERED_ERROR);
                            event.putExtra(
                                    ReactNativeConstants.KEY_INTENT_EVENT_TYPE,
                                    ReactNativeConstants.INTENT_EVENT_TYPE_STRING);
                            event.putExtra(ReactNativeConstants.KEY_INTENT_EVENT_STRING_DATA, e.getMessage());
                            ReactNativeNotificationsHandler.sendBroadcast(
                                    ReactNativeRegistrationIntentService.this, event, 0);
                        }
                    }
                });
    }
}
