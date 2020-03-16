package com.azure.reactnative.notificationhub;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.microsoft.windowsazure.messaging.NotificationHub;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.azure.reactnative.notificationhub.ReactNativeConstants.*;

public final class ReactNativeUtil {
    public static final String TAG = "ReactNativeUtil";

    private static final ExecutorService mPool = Executors.newFixedThreadPool(2);

    public static void runInWorkerThread(Runnable runnable) {
        mPool.execute(runnable);
    }

    public static NotificationHub createNotificationHub(String hubName, String connectionString, Context context) {
        NotificationHub hub = new NotificationHub(hubName, connectionString, context);
        return hub;
    }

    public static JSONObject convertBundleToJSON(Bundle bundle) {
        JSONObject json = new JSONObject();
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            try {
                json.put(key, bundle.get(key));
            } catch (JSONException e) {
            }
        }

        return json;
    }

    public static WritableMap convertBundleToMap(Bundle bundle) {
        WritableMap map = Arguments.createMap();
        if (bundle == null) {
            return map;
        }

        for (String key : bundle.keySet()) {
            try {
                Object o = bundle.get(key);

                // TODO: Handle char and all array types
                if (o == null) {
                    map.putNull(key);
                } else if (o instanceof Bundle) {
                    map.putMap(key, convertBundleToMap((Bundle) o));
                } else if (o instanceof String) {
                    map.putString(key, (String) o);
                } else if (o instanceof Float) {
                    map.putDouble(key, ((Float) o).doubleValue());
                } else if (o instanceof Double) {
                    map.putDouble(key, (Double) o);
                } else if (o instanceof Integer) {
                    map.putInt(key, (Integer) o);
                } else if (o instanceof Long) {
                    map.putInt(key, ((Long) o).intValue());
                } else if (o instanceof Short) {
                    map.putInt(key, ((Short) o).intValue());
                } else if (o instanceof Byte) {
                    map.putInt(key, ((Byte) o).intValue());
                } else if (o instanceof Boolean) {
                    map.putBoolean(key, (Boolean) o);
                } else {
                    map.putNull(key);
                }
            } catch (ClassCastException e) {
                // TODO: Warn
            }
        }

        return map;
    }

    public static Intent createBroadcastIntent(String action, Bundle bundle) {
        Intent intent = ReactNativeNotificationHubUtil.IntentFactory.createIntent(action);
        intent.putExtra(KEY_INTENT_EVENT_NAME, EVENT_REMOTE_NOTIFICATION_RECEIVED);
        intent.putExtra(KEY_INTENT_EVENT_TYPE, INTENT_EVENT_TYPE_BUNDLE);
        intent.putExtras(bundle);

        return intent;
    }

    public static void emitEvent(ReactContext reactContext,
                                 String eventName,
                                 @Nullable Object params) {
        if (reactContext.hasActiveCatalystInstance()) {
            reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        }
    }

    public static void emitIntent(ReactContext reactContext,
                                  Intent intent) {
        String eventName = intent.getStringExtra(KEY_INTENT_EVENT_NAME);
        String eventType = intent.getStringExtra(KEY_INTENT_EVENT_TYPE);
        if (eventType.equals(INTENT_EVENT_TYPE_BUNDLE)) {
            WritableMap map = convertBundleToMap(intent.getExtras());
            emitEvent(reactContext, eventName, map);
        } else {
            String data = intent.getStringExtra(KEY_INTENT_EVENT_STRING_DATA);
            emitEvent(reactContext, eventName, data);
        }
    }

    public static Class getMainActivityClass(Context context) {
        String packageName = context.getPackageName();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        String className = launchIntent.getComponent().getClassName();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, ERROR_ACTIVITY_CLASS_NOT_FOUND, e);
            return null;
        }
    }

    public static int getNotificationCompatPriority(String priorityString) {
        int priority = NotificationCompat.PRIORITY_DEFAULT;
        if (priorityString != null) {
            switch (priorityString.toLowerCase()) {
                case REMOTE_NOTIFICATION_PRIORITY_MAX:
                    priority = NotificationCompat.PRIORITY_MAX;
                    break;
                case REMOTE_NOTIFICATION_PRIORITY_HIGH:
                    priority = NotificationCompat.PRIORITY_HIGH;
                    break;
                case REMOTE_NOTIFICATION_PRIORITY_LOW:
                    priority = NotificationCompat.PRIORITY_LOW;
                    break;
                case REMOTE_NOTIFICATION_PRIORITY_MIN:
                    priority = NotificationCompat.PRIORITY_MIN;
                    break;
                case REMOTE_NOTIFICATION_PRIORITY_NORMAL:
                    priority = NotificationCompat.PRIORITY_DEFAULT;
                    break;
            }
        }

        return priority;
    }

    public static int getSmallIcon(Bundle bundle, Resources res, String packageName) {
        int smallIconResId;
        String smallIcon = bundle.getString(KEY_REMOTE_NOTIFICATION_SMALL_ICON);

        if (smallIcon != null) {
            smallIconResId = res.getIdentifier(smallIcon, RESOURCE_DEF_TYPE_MIPMAP, packageName);
        } else {
            smallIconResId = res.getIdentifier(RESOURCE_NAME_NOTIFICATION, RESOURCE_DEF_TYPE_MIPMAP, packageName);
        }

        if (smallIconResId == 0) {
            smallIconResId = res.getIdentifier(RESOURCE_NAME_LAUNCHER, RESOURCE_DEF_TYPE_MIPMAP, packageName);

            if (smallIconResId == 0) {
                smallIconResId = android.R.drawable.ic_dialog_info;
            }
        }

        return smallIconResId;
    }

    public static int getLargeIcon(Bundle bundle, String largeIcon, Resources res, String packageName) {
        int largeIconResId;

        if (largeIcon != null) {
            largeIconResId = res.getIdentifier(largeIcon, RESOURCE_DEF_TYPE_MIPMAP, packageName);
        } else {
            largeIconResId = res.getIdentifier(RESOURCE_NAME_LAUNCHER, RESOURCE_DEF_TYPE_MIPMAP, packageName);
        }

        return largeIconResId;
    }

    public static Uri getSoundUri(Context context, Bundle bundle) {
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        String soundName = bundle.getString(KEY_REMOTE_NOTIFICATION_SOUND_NAME);
        if (soundName != null) {
            if (!"default".equalsIgnoreCase(soundName)) {

                // sound name can be full filename, or just the resource name.
                // So the strings 'my_sound.mp3' AND 'my_sound' are accepted
                // The reason is to make the iOS and android javascript interfaces compatible

                int resId = context.getResources().getIdentifier(soundName, RESOURCE_DEF_TYPE_RAW, context.getPackageName());
                if (resId == 0) {
                    soundName = soundName.substring(0, soundName.lastIndexOf('.'));
                    resId = context.getResources().getIdentifier(soundName, RESOURCE_DEF_TYPE_RAW, context.getPackageName());
                }

                soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + resId);
            }
        }

        return soundUri;
    }

    public static Intent createNotificationIntent(Context context, Bundle bundle, Class intentClass) {
        Intent intent = ReactNativeNotificationHubUtil.IntentFactory.createIntent(context, intentClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        bundle.putBoolean(KEY_REMOTE_NOTIFICATION_FOREGROUND, true);
        bundle.putBoolean(KEY_REMOTE_NOTIFICATION_USER_INTERACTION, false);
        bundle.putBoolean(KEY_REMOTE_NOTIFICATION_COLDSTART, false);
        intent.putExtra(KEY_NOTIFICATION_PAYLOAD_TYPE, bundle);

        return intent;
    }

    public static void processNotificationActions(Context context, Bundle bundle,
                                           NotificationCompat.Builder notification,
                                           int notificationID) {
        JSONArray actionsArray = null;
        try {
            actionsArray = bundle.getString(KEY_REMOTE_NOTIFICATION_ACTIONS) != null ?
                    new JSONArray(bundle.getString(KEY_REMOTE_NOTIFICATION_ACTIONS)) : null;
        } catch (JSONException e) {
            Log.e(TAG, ERROR_COVERT_ACTIONS, e);
        }

        if (actionsArray != null) {
            // No icon for now. The icon value of 0 shows no icon.
            int icon = 0;

            // Add button for each actions.
            for (int i = 0; i < actionsArray.length(); i++) {
                String action;
                try {
                    action = actionsArray.getString(i);
                } catch (JSONException e) {
                    Log.e(TAG, ERROR_GET_ACTIONS_ARRAY, e);
                    continue;
                }

                Intent actionIntent = ReactNativeNotificationHubUtil.IntentFactory.createIntent();
                actionIntent.setAction(context.getPackageName() + "." + action);
                // Add "action" for later identifying which button gets pressed.
                bundle.putString(KEY_REMOTE_NOTIFICATION_ACTION, action);
                actionIntent.putExtra(KEY_NOTIFICATION_PAYLOAD_TYPE, bundle);
                PendingIntent pendingActionIntent = PendingIntent.getBroadcast(context, notificationID, actionIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                notification.addAction(icon, action, pendingActionIntent);
            }
        }
    }

    public static NotificationCompat.Builder initNotificationCompatBuilder(Context context,
                                                                    String notificationChannelID,
                                                                    String title,
                                                                    CharSequence ticker,
                                                                    int visibility,
                                                                    int priority,
                                                                    boolean autoCancel) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, notificationChannelID)
                .setContentTitle(title)
                .setTicker(ticker)
                .setVisibility(visibility)
                .setPriority(priority)
                .setAutoCancel(autoCancel);

        return notificationBuilder;
    }

    public static Bundle getBundleFromIntent(Intent intent) {
        Bundle bundle = null;
        if (intent.hasExtra(KEY_NOTIFICATION_PAYLOAD_TYPE)) {
            bundle = intent.getBundleExtra(KEY_NOTIFICATION_PAYLOAD_TYPE);
        } else if (intent.hasExtra(KEY_REMOTE_NOTIFICATION_ID)) {
            bundle = intent.getExtras();
        }

        return bundle;
    }

    public static void removeNotificationFromIntent(Intent intent) {
        if (intent.hasExtra(KEY_NOTIFICATION_PAYLOAD_TYPE)) {
            intent.removeExtra(KEY_NOTIFICATION_PAYLOAD_TYPE);
        } else if (intent.hasExtra(KEY_REMOTE_NOTIFICATION_ID)) {
            intent.removeExtra(KEY_REMOTE_NOTIFICATION_ID);
        }
    }

    public static NotificationCompat.BigTextStyle getBigTextStyle(String bigText) {
        return new NotificationCompat.BigTextStyle().bigText(bigText);
    }

    public static class UrlWrapper {
        public static HttpURLConnection openConnection(String url) throws Exception {
            return (HttpURLConnection)(new URL(url)).openConnection();
        }
    }

    public static Bitmap fetchImage(String urlString) {
        try {
            HttpURLConnection connection = UrlWrapper.openConnection(urlString);
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            Log.e(TAG, ERROR_FETCH_IMAGE, e);
            return null;
        }
    }

    public static String genUUID() {
        return UUID.randomUUID().toString();
    }

    private ReactNativeUtil() {
    }
}
