package com.azure.reactnative.notificationhub;

import android.app.Activity;
import android.content.Intent;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import com.microsoft.windowsazure.messaging.NotificationHub;
import com.microsoft.windowsazure.notifications.NotificationsManager;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.UiThreadUtil;

public class ReactNativeNotificationHubModule extends ReactContextBaseJavaModule {
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private static final String ERROR_INVALID_ARGUMENTS = "E_INVALID_ARGUMENTS";
    private static final String ERROR_PLAY_SERVICES = "E_PLAY_SERVICES";
    private static final String ERROR_NOTIFICATION_HUB = "E_NOTIFICATION_HUB";
    private static final String ERROR_NOT_REGISTERED = "E_NOT_REGISTERED";

    public ReactNativeNotificationHubModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "AzureNotificationHub";
    }

    @ReactMethod
    public void register(ReadableMap config, Promise promise) {
        NotificationHubUtil notificationHubUtil = NotificationHubUtil.getInstance();
        String connectionString = config.getString("connectionString");
        if (connectionString == null) {
            promise.reject(ERROR_INVALID_ARGUMENTS, "Connection string cannot be null.");
        }

        String hubName = config.getString("hubName");
        if (hubName == null) {
            promise.reject(ERROR_INVALID_ARGUMENTS, "Hub name cannot be null.");
        }

        String senderID = config.getString("senderID");
        if (senderID == null) {
            promise.reject(ERROR_INVALID_ARGUMENTS, "Sender ID cannot be null.");
        }

        ReadableArray tagsJson = config.getArray("tags");
        String[] tags = null;
        if (tagsJson != null) {
            tags = new String[tagsJson.size()];
            for (int i = 0; i < tagsJson.size(); ++i) {
                tags[i] = tagsJson.getString(i);
            }
        }

        ReactContext reactContext = getReactApplicationContext();
        notificationHubUtil.setConnectionString(reactContext, connectionString);
        notificationHubUtil.setHubName(reactContext, hubName);
        notificationHubUtil.setTags(reactContext, tags);

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(reactContext);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                UiThreadUtil.runOnUiThread(
                        new GoogleApiAvailabilityRunnable(
                                getCurrentActivity(),
                                apiAvailability,
                                resultCode));
                promise.reject(ERROR_PLAY_SERVICES, "User must enable Google Play Services.");
            } else {
                promise.reject(ERROR_PLAY_SERVICES, "This device is not supported by Google Play Services.");
            }
            return;
        }

        Intent intent = new Intent(reactContext, ReactNativeRegistrationIntentService.class);
        reactContext.startService(intent);
        NotificationsManager.handleNotifications(reactContext, senderID, ReactNativeNotificationsHandler.class);
    }

    @ReactMethod
    public void unregister(Promise promise) {
        NotificationHubUtil notificationHubUtil = NotificationHubUtil.getInstance();

        ReactContext reactContext = getReactApplicationContext();
        String connectionString = notificationHubUtil.getConnectionString(reactContext);
        String hubName = notificationHubUtil.getHubName(reactContext);
        String registrationId = notificationHubUtil.getRegistrationID(reactContext);

        if (connectionString == null || hubName == null || registrationId == null) {
            promise.reject(ERROR_NOT_REGISTERED, "No registration to Azure Notification Hub.");
        }

        NotificationHub hub = new NotificationHub(hubName, connectionString, reactContext);
        try {
            hub.unregister();
        } catch (Exception e) {
            promise.reject(ERROR_NOTIFICATION_HUB, e);
        }

        NotificationsManager.stopHandlingNotifications(reactContext);
    }

    private static class GoogleApiAvailabilityRunnable implements Runnable {
        private final Activity activity;
        private final GoogleApiAvailability apiAvailability;
        private final int resultCode;

        public GoogleApiAvailabilityRunnable(
                Activity activity,
                GoogleApiAvailability apiAvailability,
                int resultCode) {
            this.activity = activity;
            this.apiAvailability = apiAvailability;
            this.resultCode = resultCode;
        }

        @Override
        public void run() {
            apiAvailability.getErrorDialog(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
        }
    }
}
