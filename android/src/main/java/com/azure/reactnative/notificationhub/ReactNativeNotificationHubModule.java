package com.azure.reactnative.notificationhub;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.WritableMap;
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
    public void getInitialNotification(Promise promise) {
        Activity activity = getCurrentActivity();
        if (activity == null) {
            promise.reject(ERROR_GET_INIT_NOTIFICATION, ERROR_ACTIVITY_IS_NULL);
            return;
        }

        Intent intent = activity.getIntent();
        if (intent != null && intent.getAction() != null) {
            if (intent.getExtras() == null) {
                // In certain cases while app cold launches, i.getExtras() returns null.
                // Adding the check to make sure app won't crash,
                // and still successfully launches from notification
                promise.reject(ERROR_GET_INIT_NOTIFICATION, ERROR_INTENT_EXTRAS_IS_NULL);
            } else {
                promise.resolve(ReactNativeUtil.convertBundleToMap(intent.getExtras()));
            }
        } else {
            promise.reject(ERROR_GET_INIT_NOTIFICATION, ERROR_ACTIVITY_INTENT_IS_NULL);
        }
    }

    @ReactMethod
    public void register(ReadableMap config, Promise promise) {
        ReactNativeNotificationHubUtil notificationHubUtil = ReactNativeNotificationHubUtil.getInstance();
        String connectionString = config.getString(KEY_REGISTRATION_CONNECTIONSTRING);
        if (connectionString == null) {
            promise.reject(ERROR_INVALID_ARGUMENTS, ERROR_INVALID_CONNECTION_STRING);
            return;
        }

        String hubName = config.getString(KEY_REGISTRATION_HUBNAME);
        if (hubName == null) {
            promise.reject(ERROR_INVALID_ARGUMENTS, ERROR_INVALID_HUBNAME);
            return;
        }

        String senderID = config.getString(KEY_REGISTRATION_SENDERID);
        if (senderID == null) {
            promise.reject(ERROR_INVALID_ARGUMENTS, ERROR_INVALID_SENDER_ID);
            return;
        }

        String[] tags = null;
        if (config.hasKey(KEY_REGISTRATION_TAGS) && !config.isNull(KEY_REGISTRATION_TAGS)) {
            ReadableArray tagsJson = config.getArray(KEY_REGISTRATION_TAGS);
            tags = new String[tagsJson.size()];
            for (int i = 0; i < tagsJson.size(); ++i) {
                tags[i] = tagsJson.getString(i);
            }
        }

        ReactContext reactContext = getReactApplicationContext();
        notificationHubUtil.setConnectionString(reactContext, connectionString);
        notificationHubUtil.setHubName(reactContext, hubName);
        notificationHubUtil.setSenderID(reactContext, senderID);
        notificationHubUtil.setTemplated(reactContext, false);
        notificationHubUtil.setTags(reactContext, tags);

        if (config.hasKey(KEY_REGISTRATION_CHANNELNAME)) {
            String channelName = config.getString(KEY_REGISTRATION_CHANNELNAME);
            notificationHubUtil.setChannelName(reactContext, channelName);
        }

        if (config.hasKey(KEY_REGISTRATION_CHANNELDESCRIPTION)) {
            String channelDescription = config.getString(KEY_REGISTRATION_CHANNELDESCRIPTION);
            notificationHubUtil.setChannelDescription(reactContext, channelDescription);
        }

        if (config.hasKey(KEY_REGISTRATION_CHANNELIMPORTANCE)) {
            int channelImportance = config.getInt(KEY_REGISTRATION_CHANNELIMPORTANCE);
            notificationHubUtil.setChannelImportance(reactContext, channelImportance);
        }

        if (config.hasKey(KEY_REGISTRATION_CHANNELSHOWBADGE)) {
            boolean channelShowBadge = config.getBoolean(KEY_REGISTRATION_CHANNELSHOWBADGE);
            notificationHubUtil.setChannelShowBadge(reactContext, channelShowBadge);
        }

        if (config.hasKey(KEY_REGISTRATION_CHANNELENABLELIGHTS)) {
            boolean channelEnableLights = config.getBoolean(KEY_REGISTRATION_CHANNELENABLELIGHTS);
            notificationHubUtil.setChannelEnableLights(reactContext, channelEnableLights);
        }

        if (config.hasKey(KEY_REGISTRATION_CHANNELENABLEVIBRATION)) {
            boolean channelEnableVibration = config.getBoolean(KEY_REGISTRATION_CHANNELENABLEVIBRATION);
            notificationHubUtil.setChannelEnableVibration(reactContext, channelEnableVibration);
        }

        String uuid = notificationHubUtil.getUUID(reactContext);
        if (uuid == null) {
            uuid = ReactNativeUtil.genUUID();
            notificationHubUtil.setUUID(reactContext, uuid);
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

        WritableMap res = Arguments.createMap();
        res.putString(KEY_PROMISE_RESOLVE_UUID, uuid);
        promise.resolve(res);
    }

    @ReactMethod
    public void registerTemplate(ReadableMap config, Promise promise) {
        ReactNativeNotificationHubUtil notificationHubUtil = ReactNativeNotificationHubUtil.getInstance();
        String connectionString = config.getString(KEY_REGISTRATION_CONNECTIONSTRING);
        if (connectionString == null) {
            promise.reject(ERROR_INVALID_ARGUMENTS, ERROR_INVALID_CONNECTION_STRING);
            return;
        }

        String hubName = config.getString(KEY_REGISTRATION_HUBNAME);
        if (hubName == null) {
            promise.reject(ERROR_INVALID_ARGUMENTS, ERROR_INVALID_HUBNAME);
            return;
        }

        String senderID = config.getString(KEY_REGISTRATION_SENDERID);
        if (senderID == null) {
            promise.reject(ERROR_INVALID_ARGUMENTS, ERROR_INVALID_SENDER_ID);
            return;
        }

        String templateName = config.getString(KEY_REGISTRATION_TEMPLATENAME);
        if (templateName == null) {
            promise.reject(ERROR_INVALID_ARGUMENTS, ERROR_INVALID_TEMPLATE_NAME);
            return;
        }

        String template = config.getString(KEY_REGISTRATION_TEMPLATE);
        if (template == null) {
            promise.reject(ERROR_INVALID_ARGUMENTS, ERROR_INVALID_TEMPLATE);
            return;
        }

        String[] tags = null;
        if (config.hasKey(KEY_REGISTRATION_TAGS) && !config.isNull(KEY_REGISTRATION_TAGS)) {
            ReadableArray tagsJson = config.getArray(KEY_REGISTRATION_TAGS);
            tags = new String[tagsJson.size()];
            for (int i = 0; i < tagsJson.size(); ++i) {
                tags[i] = tagsJson.getString(i);
            }
        }

        ReactContext reactContext = getReactApplicationContext();
        notificationHubUtil.setConnectionString(reactContext, connectionString);
        notificationHubUtil.setHubName(reactContext, hubName);
        notificationHubUtil.setSenderID(reactContext, senderID);
        notificationHubUtil.setTemplateName(reactContext, templateName);
        notificationHubUtil.setTemplate(reactContext, template);
        notificationHubUtil.setTemplated(reactContext, true);
        notificationHubUtil.setTags(reactContext, tags);

        if (config.hasKey(KEY_REGISTRATION_CHANNELNAME)) {
            String channelName = config.getString(KEY_REGISTRATION_CHANNELNAME);
            notificationHubUtil.setChannelName(reactContext, channelName);
        }

        if (config.hasKey(KEY_REGISTRATION_CHANNELDESCRIPTION)) {
            String channelDescription = config.getString(KEY_REGISTRATION_CHANNELDESCRIPTION);
            notificationHubUtil.setChannelDescription(reactContext, channelDescription);
        }

        if (config.hasKey(KEY_REGISTRATION_CHANNELIMPORTANCE)) {
            int channelImportance = config.getInt(KEY_REGISTRATION_CHANNELIMPORTANCE);
            notificationHubUtil.setChannelImportance(reactContext, channelImportance);
        }

        if (config.hasKey(KEY_REGISTRATION_CHANNELSHOWBADGE)) {
            boolean channelShowBadge = config.getBoolean(KEY_REGISTRATION_CHANNELSHOWBADGE);
            notificationHubUtil.setChannelShowBadge(reactContext, channelShowBadge);
        }

        if (config.hasKey(KEY_REGISTRATION_CHANNELENABLELIGHTS)) {
            boolean channelEnableLights = config.getBoolean(KEY_REGISTRATION_CHANNELENABLELIGHTS);
            notificationHubUtil.setChannelEnableLights(reactContext, channelEnableLights);
        }

        if (config.hasKey(KEY_REGISTRATION_CHANNELENABLEVIBRATION)) {
            boolean channelEnableVibration = config.getBoolean(KEY_REGISTRATION_CHANNELENABLEVIBRATION);
            notificationHubUtil.setChannelEnableVibration(reactContext, channelEnableVibration);
        }

        String uuid = notificationHubUtil.getUUID(reactContext);
        if (uuid == null) {
            uuid = ReactNativeUtil.genUUID();
            notificationHubUtil.setUUID(reactContext, uuid);
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

        WritableMap res = Arguments.createMap();
        res.putString(KEY_PROMISE_RESOLVE_UUID, uuid);
        promise.resolve(res);
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
            notificationHubUtil.setUUID(reactContext, null);
            promise.resolve(AZURE_NOTIFICATION_HUB_UNREGISTERED);
        } catch (Exception e) {
            promise.reject(ERROR_NOTIFICATION_HUB, e);
        }
    }

    @ReactMethod
    public void unregisterTemplate(String templateName, Promise promise) {
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
            hub.unregisterTemplate(templateName);
            notificationHubUtil.setRegistrationID(reactContext, null);
            notificationHubUtil.setUUID(reactContext, null);
            promise.resolve(AZURE_NOTIFICATION_HUB_UNREGISTERED);
        } catch (Exception e) {
            promise.reject(ERROR_NOTIFICATION_HUB, e);
        }
    }

    @ReactMethod
    public void getUUID(Boolean autoGen, Promise promise) {
        ReactNativeNotificationHubUtil notificationHubUtil = ReactNativeNotificationHubUtil.getInstance();
        ReactContext reactContext = getReactApplicationContext();
        String uuid = notificationHubUtil.getUUID(reactContext);

        if (uuid != null) {
            promise.resolve(uuid);
        } else if (autoGen) {
            uuid = ReactNativeUtil.genUUID();
            notificationHubUtil.setUUID(reactContext, uuid);
            promise.resolve(uuid);
        } else {
            promise.reject(ERROR_GET_UUID, ERROR_NO_UUID_SET);
        }
    }

    @ReactMethod
    public void isNotificationEnabledOnOSLevel(Promise promise) {
        ReactContext reactContext = getReactApplicationContext();
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(reactContext);
        boolean areNotificationsEnabled = notificationManagerCompat.areNotificationsEnabled();
        promise.resolve(areNotificationsEnabled);
    }

    @Override
    public void onHostResume() {
        setIsForeground(true);

        Activity activity = getCurrentActivity();
        if (activity != null) {
            Intent intent = activity.getIntent();
            if (intent != null) {
                Bundle bundle = ReactNativeUtil.getBundleFromIntent(intent);
                if (bundle != null) {
                    ReactNativeUtil.removeNotificationFromIntent(intent);
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
        Bundle bundle = ReactNativeUtil.getBundleFromIntent(intent);
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
                ReactNativeUtil.emitIntent(mReactContext, intent);
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
