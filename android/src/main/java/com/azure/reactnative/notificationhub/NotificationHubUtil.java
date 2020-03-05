package com.azure.reactnative.notificationhub;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.facebook.react.bridge.ReactContext;
import com.microsoft.windowsazure.messaging.NotificationHub;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.ERROR_COVERT_ACTIONS;
import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.ERROR_GET_ACTIONS_ARRAY;
import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.KEY_INTENT_NOTIFICATION;
import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.KEY_REMOTE_NOTIFICATION_ACTION;
import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.KEY_REMOTE_NOTIFICATION_ACTIONS;
import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.KEY_REMOTE_NOTIFICATION_COLDSTART;
import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.KEY_REMOTE_NOTIFICATION_FOREGROUND;
import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.KEY_REMOTE_NOTIFICATION_SMALL_ICON;
import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.KEY_REMOTE_NOTIFICATION_SOUND_NAME;
import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.KEY_REMOTE_NOTIFICATION_USER_INTERACTION;
import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.REMOTE_NOTIFICATION_PRIORITY_HIGH;
import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.REMOTE_NOTIFICATION_PRIORITY_LOW;
import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.REMOTE_NOTIFICATION_PRIORITY_MAX;
import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.REMOTE_NOTIFICATION_PRIORITY_MIN;
import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.REMOTE_NOTIFICATION_PRIORITY_NORMAL;
import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.RESOURCE_DEF_TYPE_MIPMAP;
import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.RESOURCE_DEF_TYPE_RAW;
import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.RESOURCE_NAME_LAUNCHER;
import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.RESOURCE_NAME_NOTIFICATION;

public class NotificationHubUtil {
    private static NotificationHubUtil sharedNotificationHubUtilInstance = null;

    private static final String SHARED_PREFS_NAME = "com.azure.reactnative.notificationhub.NotificationHubUtil";
    private static final String KEY_FOR_PREFS_REGISTRATIONID = "AzureNotificationHub_registrationID";
    private static final String KEY_FOR_PREFS_CONNECTIONSTRING = "AzureNotificationHub_connectionString";
    private static final String KEY_FOR_PREFS_HUBNAME = "AzureNotificationHub_hubName";
    private static final String KEY_FOR_PREFS_FCMTOKEN = "AzureNotificationHub_FCMToken";
    private static final String KEY_FOR_PREFS_TAGS = "AzureNotificationHub_Tags";
    private static final String KEY_FOR_PREFS_SENDERID = "AzureNotificationHub_senderID";
    private static final String KEY_FOR_PREFS_CHANNELIMPORTANCE = "AzureNotificationHub_channelImportance";
    private static final String KEY_FOR_PREFS_CHANNELSHOWBADGE = "AzureNotificationHub_channelShowBadge";
    private static final String KEY_FOR_PREFS_CHANNELENABLELIGHTS = "AzureNotificationHub_channelEnableLights";
    private static final String KEY_FOR_PREFS_CHANNELENABLEVIBRATION = "AzureNotificationHub_channelEnableVibration";

    private final ExecutorService mPool = Executors.newFixedThreadPool(1);

    private boolean mIsForeground;

    public static NotificationHubUtil getInstance() {
        if (sharedNotificationHubUtilInstance == null) {
            sharedNotificationHubUtilInstance = new NotificationHubUtil();
        }
        return sharedNotificationHubUtilInstance;
    }

    public String getConnectionString(Context context) {
        return getPref(context, KEY_FOR_PREFS_CONNECTIONSTRING);
    }

    public void setConnectionString(Context context, String connectionString) {
        setPref(context, KEY_FOR_PREFS_CONNECTIONSTRING, connectionString);
    }

    public String getHubName(Context context) {
        return getPref(context, KEY_FOR_PREFS_HUBNAME);
    }

    public void setHubName(Context context, String hubName) {
        setPref(context, KEY_FOR_PREFS_HUBNAME, hubName);
    }

    public String getRegistrationID(Context context) {
        return getPref(context, KEY_FOR_PREFS_REGISTRATIONID);
    }

    public void setRegistrationID(Context context, String registrationID) {
        setPref(context, KEY_FOR_PREFS_REGISTRATIONID, registrationID);
    }

    public String getFCMToken(Context context) {
        return getPref(context, KEY_FOR_PREFS_FCMTOKEN);
    }

    public void setFCMToken(Context context, String token) {
        setPref(context, KEY_FOR_PREFS_FCMTOKEN, token);
    }

    public String[] getTags(Context context) {
        Set<String> set = getPrefSet(context, KEY_FOR_PREFS_TAGS);
        return set != null ? set.toArray(new String[set.size()]) : null;
    }

    public void setTags(Context context, String[] tags) {
        Set<String> set = tags != null ? new HashSet<>(Arrays.asList(tags)) : null;
        setPrefSet(context, KEY_FOR_PREFS_TAGS, set);
    }

    public String getSenderID(Context context) {
        return getPref(context, KEY_FOR_PREFS_SENDERID);
    }

    public void setSenderID(Context context, String senderID) {
        setPref(context, KEY_FOR_PREFS_SENDERID, senderID);
    }

    public int getChannelImportance(Context context) {
        return getPrefInt(context, KEY_FOR_PREFS_CHANNELIMPORTANCE);
    }

    public void setChannelImportance(Context context, int channelImportance) {
        setPrefInt(context, KEY_FOR_PREFS_CHANNELIMPORTANCE, channelImportance);
    }

    public boolean hasChannelImportance(Context context) {
        return hasKey(context, KEY_FOR_PREFS_CHANNELIMPORTANCE);
    }

    public boolean getChannelShowBadge(Context context) {
        return getPrefBoolean(context, KEY_FOR_PREFS_CHANNELSHOWBADGE);
    }

    public void setChannelShowBadge(Context context, boolean channelShowBadge) {
        setPrefBoolean(context, KEY_FOR_PREFS_CHANNELSHOWBADGE, channelShowBadge);
    }

    public boolean hasChannelShowBadge(Context context) {
        return hasKey(context, KEY_FOR_PREFS_CHANNELSHOWBADGE);
    }

    public boolean getChannelEnableLights(Context context) {
        return getPrefBoolean(context, KEY_FOR_PREFS_CHANNELENABLELIGHTS);
    }

    public void setChannelEnableLights(Context context, boolean channelEnableLights) {
        setPrefBoolean(context, KEY_FOR_PREFS_CHANNELENABLELIGHTS, channelEnableLights);
    }

    public boolean hasChannelEnableLights(Context context) {
        return hasKey(context, KEY_FOR_PREFS_CHANNELENABLELIGHTS);
    }

    public boolean getChannelEnableVibration(Context context) {
        return getPrefBoolean(context, KEY_FOR_PREFS_CHANNELENABLEVIBRATION);
    }

    public void setChannelEnableVibration(Context context, boolean channelEnableVibration) {
        setPrefBoolean(context, KEY_FOR_PREFS_CHANNELENABLEVIBRATION, channelEnableVibration);
    }

    public boolean hasChannelEnableVibration(Context context) {
        return hasKey(context, KEY_FOR_PREFS_CHANNELENABLEVIBRATION);
    }

    public void setAppIsForeground(boolean isForeground) {
        mIsForeground = isForeground;
    }

    public boolean getAppIsForeground() {
        return mIsForeground;
    }

    public void runInWorkerThread(Runnable runnable) {
        mPool.execute(runnable);
    }

    public NotificationHub createNotificationHub(String hubName, String connectionString, ReactContext reactContext) {
        NotificationHub hub = new NotificationHub(hubName, connectionString, reactContext);
        return hub;
    }

    public JSONObject convertBundleToJSON(Bundle bundle) {
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

    public Intent createBroadcastIntent(String action, JSONObject json) {
        Intent intent = new Intent(action);
        intent.putExtra("event", ReactNativeNotificationHubModule.DEVICE_NOTIF_EVENT);
        intent.putExtra("data", json.toString());

        return intent;
    }

    public Class getMainActivityClass(Context context) {
        String packageName = context.getPackageName();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        String className = launchIntent.getComponent().getClassName();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getNotificationCompatPriority(String priorityString) {
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

    public int getSmallIcon(Bundle bundle, Resources res, String packageName) {
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

    public int getLargeIcon(Bundle bundle, String largeIcon, Resources res, String packageName) {
        int largeIconResId;

        if (largeIcon != null) {
            largeIconResId = res.getIdentifier(largeIcon, RESOURCE_DEF_TYPE_MIPMAP, packageName);
        } else {
            largeIconResId = res.getIdentifier(RESOURCE_NAME_LAUNCHER, RESOURCE_DEF_TYPE_MIPMAP, packageName);
        }

        return largeIconResId;
    }

    public Uri getSoundUri(Context context, Bundle bundle) {
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        String soundName = bundle.getString(KEY_REMOTE_NOTIFICATION_SOUND_NAME);
        if (soundName != null) {
            if (!"default".equalsIgnoreCase(soundName)) {

                // sound name can be full filename, or just the resource name.
                // So the strings 'my_sound.mp3' AND 'my_sound' are accepted
                // The reason is to make the iOS and android javascript interfaces compatible

                int resId;
                if (context.getResources().getIdentifier(soundName, RESOURCE_DEF_TYPE_RAW, context.getPackageName()) != 0) {
                    resId = context.getResources().getIdentifier(soundName, RESOURCE_DEF_TYPE_RAW, context.getPackageName());
                } else {
                    soundName = soundName.substring(0, soundName.lastIndexOf('.'));
                    resId = context.getResources().getIdentifier(soundName, RESOURCE_DEF_TYPE_RAW, context.getPackageName());
                }

                soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + resId);
            }
        }

        return soundUri;
    }

    public Intent createNotificationIntent(Context context, Bundle bundle, Class intentClass) {
        Intent intent = new Intent(context, intentClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        bundle.putBoolean(KEY_REMOTE_NOTIFICATION_FOREGROUND, true);
        bundle.putBoolean(KEY_REMOTE_NOTIFICATION_USER_INTERACTION, false);
        bundle.putBoolean(KEY_REMOTE_NOTIFICATION_COLDSTART, false);
        intent.putExtra(KEY_INTENT_NOTIFICATION, bundle);

        return intent;
    }

    public void processNotificationActions(Context context, Bundle bundle,
                                           NotificationCompat.Builder notification,
                                           int notificationID) {
        final String tag = ReactNativeNotificationsHandler.TAG;
        JSONArray actionsArray = null;
        try {
            actionsArray = bundle.getString(KEY_REMOTE_NOTIFICATION_ACTIONS) != null ?
                    new JSONArray(bundle.getString(KEY_REMOTE_NOTIFICATION_ACTIONS)) : null;
        } catch (JSONException e) {
            Log.e(tag, ERROR_COVERT_ACTIONS, e);
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
                    Log.e(tag, ERROR_GET_ACTIONS_ARRAY, e);
                    continue;
                }

                Intent actionIntent = new Intent();
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

    private String getPref(Context context, String key) {
        SharedPreferences prefs =
                context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(key, null);
    }

    private int getPrefInt(Context context, String key) {
        SharedPreferences prefs =
                context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(key, 0);
    }

    private boolean getPrefBoolean(Context context, String key) {
        SharedPreferences prefs =
                context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(key, false);
    }

    private Set<String> getPrefSet(Context context, String key) {
        SharedPreferences prefs =
                context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getStringSet(key, null);
    }

    private void setPref(Context context, String key, String value) {
        SharedPreferences.Editor editor =
                context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(key, value);
        editor.apply();
    }

    private void setPrefInt(Context context, String key, int value) {
        SharedPreferences.Editor editor =
                context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(key, value);
        editor.apply();
    }

    private void setPrefBoolean(Context context, String key, boolean value) {
        SharedPreferences.Editor editor =
                context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void setPrefSet(Context context, String key, Set<String> value) {
        SharedPreferences.Editor editor =
                context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putStringSet(key, value);
        editor.apply();
    }

    private boolean hasKey(Context context, String key) {
        SharedPreferences prefs =
                context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.contains(key);
    }
}
