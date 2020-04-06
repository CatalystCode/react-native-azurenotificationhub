package com.azure.reactnative.notificationhub;

import android.app.NotificationChannel;
import android.app.NotificationManager;

public class ReactNativeNotificationChannelBuilder {
    private String mID = ReactNativeConstants.NOTIFICATION_CHANNEL_ID;
    private CharSequence mName = "rn-push-notification-channel-name";
    private int mImportance = NotificationManager.IMPORTANCE_DEFAULT;
    private boolean mShowBadge = true;
    private boolean mEnableLights = true;
    private boolean mEnableVibration = true;
    private String mDesc = null;

    public static class Factory {
        public static ReactNativeNotificationChannelBuilder create() {
            return new ReactNativeNotificationChannelBuilder();
        }
    }

    private ReactNativeNotificationChannelBuilder() {
    }

    public NotificationChannel build() {
        NotificationChannel channel = new NotificationChannel(mID, mName, mImportance);
        channel.setShowBadge(mShowBadge);
        channel.enableLights(mEnableLights);
        channel.enableVibration(mEnableVibration);

        if (mDesc != null) {
            channel.setDescription(mDesc);
        }

        return channel;
    }

    public ReactNativeNotificationChannelBuilder setName(CharSequence name) {
        this.mName = name;
        return this;
    }

    public ReactNativeNotificationChannelBuilder setImportance(int importance) {
        this.mImportance = importance;
        return this;
    }

    public ReactNativeNotificationChannelBuilder setDescription(String desc) {
        this.mDesc = desc;
        return this;
    }

    public ReactNativeNotificationChannelBuilder setShowBadge(boolean showBadge) {
        this.mShowBadge = showBadge;
        return this;
    }

    public ReactNativeNotificationChannelBuilder enableLights(boolean enableLights) {
        this.mEnableLights = enableLights;
        return this;
    }

    public ReactNativeNotificationChannelBuilder enableVibration(boolean enableVibration) {
        this.mEnableVibration = enableVibration;
        return this;
    }
}
