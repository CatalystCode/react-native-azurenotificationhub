package com.azure.reactnative.notificationhub;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.facebook.react.bridge.ReactContext;
import com.microsoft.windowsazure.messaging.NotificationHub;

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

    public NotificationHub createNotificationHub(String hubName, String connectionString, ReactContext reactContext) {
        NotificationHub hub = new NotificationHub(hubName, connectionString, reactContext);
        return hub;
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
