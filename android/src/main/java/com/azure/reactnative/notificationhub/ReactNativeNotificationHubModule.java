package com.azure.reactnative.notificationhub;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import com.microsoft.windowsazure.messaging.NotificationHub;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.UiThreadUtil;

import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.KEY_INTENT_NOTIFICATION;
import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.KEY_REMOTE_NOTIFICATION_COLDSTART;
import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.KEY_REMOTE_NOTIFICATION_FOREGROUND;
import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.KEY_REMOTE_NOTIFICATION_USER_INTERACTION;

public class ReactNativeNotificationHubModule extends ReactContextBaseJavaModule implements
        ActivityEventListener, LifecycleEventListener {
    public static final String AZURE_NOTIFICATION_HUB_NAME = "AzureNotificationHub";
    public static final String NOTIF_REGISTER_AZURE_HUB_EVENT = "azureNotificationHubRegistered";
    public static final String NOTIF_AZURE_HUB_REGISTRATION_ERROR_EVENT = "azureNotificationHubRegistrationError";
    public static final String DEVICE_NOTIF_EVENT = "remoteNotificationReceived";

    public static final String ERROR_INVALID_ARGUMENTS = "E_INVALID_ARGUMENTS";
    public static final String ERROR_INVALID_CONNECTION_STRING = "Connection string cannot be null.";
    public static final String ERROR_INVALID_HUBNAME = "Hub name cannot be null.";
    public static final String ERROR_INVALID_SENDER_ID = "Sender ID cannot be null.";
    public static final String ERROR_PLAY_SERVICES = "E_PLAY_SERVICES";
    public static final String ERROR_PLAY_SERVICES_DISABLED = "User must enable Google Play Services.";
    public static final String ERROR_PLAY_SERVICES_UNSUPPORTED = "This device is not supported by Google Play Services.";
    public static final String ERROR_NOTIFICATION_HUB = "E_NOTIFICATION_HUB";
    public static final String ERROR_NOT_REGISTERED = "E_NOT_REGISTERED";
    public static final String ERROR_NOT_REGISTERED_DESC = "No registration to Azure Notification Hub.";

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final int NOTIFICATION_DELAY_ON_START = 3000;

    private ReactApplicationContext mReactContext;
    private LocalBroadcastReceiver mLocalBroadcastReceiver;

    public ReactNativeNotificationHubModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.mReactContext = reactContext;
        this.mLocalBroadcastReceiver = new LocalBroadcastReceiver();
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(reactContext);
        localBroadcastManager.registerReceiver(mLocalBroadcastReceiver, new IntentFilter(ReactNativeRegistrationIntentService.TAG));
        localBroadcastManager.registerReceiver(mLocalBroadcastReceiver, new IntentFilter(ReactNativeNotificationsHandler.TAG));
        reactContext.addLifecycleEventListener(this);
        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return AZURE_NOTIFICATION_HUB_NAME;
    }

    public void setIsForeground(boolean isForeground) {
        NotificationHubUtil notificationHubUtil = NotificationHubUtil.getInstance();
        notificationHubUtil.setAppIsForeground(isForeground);
    }

    public boolean getIsForeground() {
        NotificationHubUtil notificationHubUtil = NotificationHubUtil.getInstance();
        return notificationHubUtil.getAppIsForeground();
    }

    @ReactMethod
    public void register(ReadableMap config, Promise promise) {
        NotificationHubUtil notificationHubUtil = NotificationHubUtil.getInstance();
        String connectionString = config.getString("connectionString");
        if (connectionString == null) {
            promise.reject(ERROR_INVALID_ARGUMENTS, ERROR_INVALID_CONNECTION_STRING);
            return;
        }

        String hubName = config.getString("hubName");
        if (hubName == null) {
            promise.reject(ERROR_INVALID_ARGUMENTS, ERROR_INVALID_HUBNAME);
            return;
        }

        String senderID = config.getString("senderID");
        if (senderID == null) {
            promise.reject(ERROR_INVALID_ARGUMENTS, ERROR_INVALID_SENDER_ID);
            return;
        }

        String[] tags = null;
        if (config.hasKey("tags") && !config.isNull("tags")) {
            ReadableArray tagsJson = config.getArray("tags");
            tags = new String[tagsJson.size()];
            for (int i = 0; i < tagsJson.size(); ++i) {
                tags[i] = tagsJson.getString(i);
            }
        }

        ReactContext reactContext = getReactApplicationContext();
        notificationHubUtil.setConnectionString(reactContext, connectionString);
        notificationHubUtil.setHubName(reactContext, hubName);
        notificationHubUtil.setSenderID(reactContext, senderID);
        notificationHubUtil.setTags(reactContext, tags);

        if (config.hasKey("channelImportance")) {
            int channelImportance = config.getInt("channelImportance");
            notificationHubUtil.setChannelImportance(reactContext, channelImportance);
        }

        if (config.hasKey("channelShowBadge")) {
            boolean channelShowBadge = config.getBoolean("channelShowBadge");
            notificationHubUtil.setChannelShowBadge(reactContext, channelShowBadge);
        }

        if (config.hasKey("channelEnableLights")) {
            boolean channelEnableLights = config.getBoolean("channelEnableLights");
            notificationHubUtil.setChannelEnableLights(reactContext, channelEnableLights);
        }

        if (config.hasKey("channelEnableVibration")) {
            boolean channelEnableVibration = config.getBoolean("channelEnableVibration");
            notificationHubUtil.setChannelEnableVibration(reactContext, channelEnableVibration);
        }

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(reactContext);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                UiThreadUtil.runOnUiThread(
                        new GoogleApiAvailabilityRunnable(
                                getCurrentActivity(),
                                apiAvailability,
                                resultCode));
                promise.reject(ERROR_PLAY_SERVICES, ERROR_PLAY_SERVICES_DISABLED);
            } else {
                promise.reject(ERROR_PLAY_SERVICES, ERROR_PLAY_SERVICES_UNSUPPORTED);
            }
            return;
        }

        Intent intent = NotificationHubUtil.IntentFactory.createIntent(
                reactContext, ReactNativeRegistrationIntentService.class);
        ReactNativeRegistrationIntentService.enqueueWork(reactContext, intent);
    }

    @ReactMethod
    public void unregister(Promise promise) {
        NotificationHubUtil notificationHubUtil = NotificationHubUtil.getInstance();

        ReactContext reactContext = getReactApplicationContext();
        String connectionString = notificationHubUtil.getConnectionString(reactContext);
        String hubName = notificationHubUtil.getHubName(reactContext);
        String registrationId = notificationHubUtil.getRegistrationID(reactContext);

        if (connectionString == null || hubName == null || registrationId == null) {
            promise.reject(ERROR_NOT_REGISTERED, ERROR_NOT_REGISTERED_DESC);
            return;
        }

        NotificationHub hub = notificationHubUtil.createNotificationHub(hubName, connectionString, reactContext);
        try {
            hub.unregister();
            notificationHubUtil.setRegistrationID(reactContext, null);
        } catch (Exception e) {
            promise.reject(ERROR_NOTIFICATION_HUB, e);
        }
    }

    @Override
    public void onHostResume() {
        setIsForeground(true);

        Activity activity = getCurrentActivity();
        if (activity != null) {
            Intent intent = activity.getIntent();
            if (intent != null) {
                Bundle bundle = intent.getBundleExtra(KEY_INTENT_NOTIFICATION);
                if (bundle != null) {
                    intent.removeExtra(KEY_INTENT_NOTIFICATION);
                    bundle.putBoolean(KEY_REMOTE_NOTIFICATION_FOREGROUND, false);
                    bundle.putBoolean(KEY_REMOTE_NOTIFICATION_USER_INTERACTION, true);
                    bundle.putBoolean(KEY_REMOTE_NOTIFICATION_COLDSTART, true);
                    ReactNativeNotificationsHandler.sendBroadcast(
                            mReactContext, bundle, NOTIFICATION_DELAY_ON_START);
                }
            }
        }
    }

    @Override
    public void onHostPause() {
        setIsForeground(false);
    }

    @Override
    public void onHostDestroy() {
    }

    @Override
    public void onNewIntent(Intent intent) {
        Bundle bundle = intent.getBundleExtra(KEY_INTENT_NOTIFICATION);
        if (bundle != null) {
            bundle.putBoolean(KEY_REMOTE_NOTIFICATION_FOREGROUND, false);
            bundle.putBoolean(KEY_REMOTE_NOTIFICATION_USER_INTERACTION, true);
            ReactNativeNotificationsHandler.sendBroadcast(
                    mReactContext, bundle, NOTIFICATION_DELAY_ON_START);
        }
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
    }

    public class LocalBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getIsForeground()) {
                String event = intent.getStringExtra("event");
                String data = intent.getStringExtra("data");
                mReactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit(event, data);
            }
        }
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
