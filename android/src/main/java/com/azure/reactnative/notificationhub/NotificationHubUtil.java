package com.azure.reactnative.notificationhub;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

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


    public static NotificationHubUtil getInstance() {
        if(sharedNotificationHubUtilInstance == null) {
            sharedNotificationHubUtilInstance = new NotificationHubUtil();
        }
        return sharedNotificationHubUtilInstance;
    }

    public String getConnectionString(Context context) {
        return getPref(context, KEY_FOR_PREFS_CONNECTIONSTRING, null);
    }

    public void setConnectionString(Context context, String connectionString) {
        setPref(context, KEY_FOR_PREFS_CONNECTIONSTRING, connectionString);
    }

    public String getHubName(Context context) {
        return getPref(context, KEY_FOR_PREFS_HUBNAME, null);
    }

    public void setHubName(Context context, String hubName) {
        setPref(context, KEY_FOR_PREFS_HUBNAME, hubName);
    }

    public String getRegistrationID(Context context) {
        return getPref(context, KEY_FOR_PREFS_REGISTRATIONID, null);
    }

    public void setRegistrationID(Context context, String registrationID) {
        setPref(context, KEY_FOR_PREFS_REGISTRATIONID, registrationID);
    }

    public String getFCMToken(Context context) {
        return getPref(context, KEY_FOR_PREFS_FCMTOKEN, null);
    }

    public void setFCMToken(Context context, String token) {
        setPref(context, KEY_FOR_PREFS_FCMTOKEN, token);
    }

    private String getPref(Context context, String key, String defaultValue) {
        SharedPreferences prefs =
                context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(key, defaultValue);
    }

    private void setPref(Context context, String key, String value) {
        SharedPreferences.Editor editor =
                context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(key, value);
        editor.apply();
    }
}