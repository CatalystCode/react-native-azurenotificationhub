package com.azure.reactnative.notificationhub;

import android.app.NotificationChannel;
import android.app.NotificationManager;

public class NotificationChannelBuilder {
    private String mID = "rn-push-notification-channel-id";
    private CharSequence mName = "rn-push-notification-channel-name";
    private String mDesc = "rn-push-notification-channel-description";
    private int mImportance = NotificationManager.IMPORTANCE_DEFAULT;
    private boolean mShowBadge = true;
    private boolean mEnableLights = true;
    private boolean mEnableVibration = true;

    public NotificationChannelBuilder() {
    }

    public NotificationChannel build() {
        NotificationChannel channel = new NotificationChannel(mID, mName, mImportance);
        channel.setDescription(mDesc);
        channel.setShowBadge(mShowBadge);
        channel.enableLights(mEnableLights);
        channel.enableVibration(mEnableVibration);
        return channel;
    }

    public NotificationChannelBuilder setName(CharSequence name) {
        this.mName = name;
        return this;
    }

    public NotificationChannelBuilder setImportance(int importance) {
        this.mImportance = importance;
        return this;
    }

    public NotificationChannelBuilder setDescription(String desc) {
        this.mDesc = desc;
        return this;
    }

    public NotificationChannelBuilder setShowBadge(boolean showBadge) {
        this.mShowBadge = showBadge;
        return this;
    }

    public NotificationChannelBuilder enableLights(boolean enableLights) {
        this.mEnableLights = enableLights;
        return this;
    }

    public NotificationChannelBuilder enableVibration(boolean enableVibration) {
        this.mEnableVibration = enableVibration;
        return this;
    }
}
