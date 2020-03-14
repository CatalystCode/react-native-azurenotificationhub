package com.azure.reactnative.notificationhub;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.facebook.react.bridge.ReactContext;
import com.microsoft.windowsazure.messaging.NotificationHub;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.azure.reactnative.notificationhub.ReactNativeConstants.*;

public final class ReactNativeUtil {
    public static final String TAG = "ReactNativeUtil";

    private static final ExecutorService mPool = Executors.newFixedThreadPool(1);

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

    public static Intent createBroadcastIntent(String action, JSONObject json) {
        Intent intent = ReactNativeNotificationHubUtil.IntentFactory.createIntent(action);
        intent.putExtra("event", DEVICE_NOTIF_EVENT);
        intent.putExtra("data", json.toString());

        return intent;
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
        intent.putExtra(KEY_INTENT_NOTIFICATION, bundle);

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
                actionIntent.putExtra(KEY_INTENT_NOTIFICATION, bundle);
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

    private ReactNativeUtil() {
    }
}
