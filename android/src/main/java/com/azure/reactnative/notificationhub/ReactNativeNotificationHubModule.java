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

import static com.azure.reactnative.notificationhub.ReactNativeConstants.*;

public class ReactNativeNotificationHubModule extends ReactContextBaseJavaModule implements
        ActivityEventListener, LifecycleEventListener {
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
        ReactNativeNotificationHubUtil notificationHubUtil = ReactNativeNotificationHubUtil.getInstance();
        notificationHubUtil.setAppIsForeground(isForeground);
    }

    public boolean getIsForeground() {
        ReactNativeNotificationHubUtil notificationHubUtil = ReactNativeNotificationHubUtil.getInstance();
        return notificationHubUtil.getAppIsForeground();
    }

    @ReactMethod
    public void register(ReadableMap config, Promise promise) {
        ReactNativeNotificationHubUtil notificationHubUtil = ReactNativeNotificationHubUtil.getInstance();
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

        Intent intent = ReactNativeNotificationHubUtil.IntentFactory.createIntent(
                reactContext, ReactNativeRegistrationIntentService.class);
        ReactNativeRegistrationIntentService.enqueueWork(reactContext, intent);
    }

    @ReactMethod
    public void unregister(Promise promise) {
        ReactNativeNotificationHubUtil notificationHubUtil = ReactNativeNotificationHubUtil.getInstance();

        ReactContext reactContext = getReactApplicationContext();
        String connectionString = notificationHubUtil.getConnectionString(reactContext);
        String hubName = notificationHubUtil.getHubName(reactContext);
        String registrationId = notificationHubUtil.getRegistrationID(reactContext);

        if (connectionString == null || hubName == null || registrationId == null) {
            promise.reject(ERROR_NOT_REGISTERED, ERROR_NOT_REGISTERED_DESC);
            return;
        }

        NotificationHub hub = ReactNativeUtil.createNotificationHub(hubName, connectionString, reactContext);
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
