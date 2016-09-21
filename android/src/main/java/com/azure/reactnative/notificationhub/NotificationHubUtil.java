package com.azure.reactnative.notificationhub;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NotificationHubUtil {
    private static NotificationHubUtil sharedNotificationHubUtilInstance = null;

    private static final String SHARED_PREFS_NAME =
            "com.azure.reactnative.notificationhub.NotificationHubUtil";
    private static final String KEY_FOR_PREFS_REGISTRATIONID =
            "AzureNotificationHub_registrationID";
    private static final String KEY_FOR_PREFS_CONNECTIONSTRING =
            "AzureNotificationHub_connectionString";
    private static final String KEY_FOR_PREFS_HUBNAME =
            "AzureNotificationHub_hubName";
    private static final String KEY_FOR_PREFS_FCMTOKEN =
            "AzureNotificationHub_FCMToken";
    private static final String KEY_FOR_PREFS_TAGS =
            "AzureNotificationHub_Tags";

    public static NotificationHubUtil getInstance() {
        if(sharedNotificationHubUtilInstance == null) {
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

    private String getPref(Context context, String key) {
        SharedPreferences prefs =
                context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(key, null);
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

    private void setPrefSet(Context context, String key, Set<String> value) {
        SharedPreferences.Editor editor =
                context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putStringSet(key, value);
        editor.apply();
    }
}
