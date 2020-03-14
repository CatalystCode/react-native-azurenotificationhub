package com.azure.reactnative.notificationhub;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;

import org.json.JSONObject;

import static com.azure.reactnative.notificationhub.ReactNativeConstants.*;

public final class ReactNativeNotificationsHandler {
    public static final String TAG = "ReactNativeNotification";

    private static final long DEFAULT_VIBRATION = 300L;

    private ReactNativeNotificationsHandler() {
    }

    public static void sendBroadcast(final Context context, final Bundle bundle, final long delay) {
        final ReactNativeNotificationHubUtil hubUtil = ReactNativeNotificationHubUtil.getInstance();
        hubUtil.runInWorkerThread(new Runnable() {
            public void run() {
                try {
                    Thread.currentThread().sleep(delay);
                    JSONObject json = hubUtil.convertBundleToJSON(bundle);
                    Intent event = hubUtil.createBroadcastIntent(TAG, json);
                    LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
                    localBroadcastManager.sendBroadcast(event);
                } catch (Exception e) {
                }
            }
        });
    }

    public static void sendNotification(Context context, Bundle bundle, String notificationChannelID) {
        try {
            final ReactNativeNotificationHubUtil hubUtil = ReactNativeNotificationHubUtil.getInstance();
            Class intentClass = hubUtil.getMainActivityClass(context);
            if (intentClass == null) {
                Log.e(TAG, ERROR_NO_ACTIVITY_CLASS);
                return;
            }

            if (bundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE) == null) {
                Log.e(TAG, ERROR_NO_MESSAGE);
                return;
            }

            String notificationIdString = bundle.getString(KEY_REMOTE_NOTIFICATION_ID);
            if (notificationIdString == null) {
                Log.e(TAG, ERROR_NO_NOTIF_ID);
                return;
            }

            Resources res = context.getResources();
            String packageName = context.getPackageName();

            String title = bundle.getString(KEY_REMOTE_NOTIFICATION_TITLE);
            if (title == null) {
                ApplicationInfo appInfo = context.getApplicationInfo();
                title = context.getPackageManager().getApplicationLabel(appInfo).toString();
            }

            int priority = hubUtil.getNotificationCompatPriority(bundle.getString(KEY_REMOTE_NOTIFICATION_PRIORITY));
            NotificationCompat.Builder notificationBuilder = hubUtil.initNotificationCompatBuilder(
                    context,
                    notificationChannelID,
                    title,
                    bundle.getString(KEY_REMOTE_NOTIFICATION_TICKER),
                    NotificationCompat.VISIBILITY_PRIVATE,
                    priority,
                    bundle.getBoolean(KEY_REMOTE_NOTIFICATION_AUTO_CANCEL, true));

            String group = bundle.getString(KEY_REMOTE_NOTIFICATION_GROUP);
            if (group != null) {
                notificationBuilder.setGroup(group);
            }

            notificationBuilder.setContentText(bundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE));

            String subText = bundle.getString(KEY_REMOTE_NOTIFICATION_SUB_TEXT);
            if (subText != null) {
                notificationBuilder.setSubText(subText);
            }

            String numberString = bundle.getString(KEY_REMOTE_NOTIFICATION_NUMBER);
            if (numberString != null) {
                notificationBuilder.setNumber(Integer.parseInt(numberString));
            }

            int smallIconResId = hubUtil.getSmallIcon(bundle, res, packageName);
            notificationBuilder.setSmallIcon(smallIconResId);

            String largeIcon = bundle.getString(KEY_REMOTE_NOTIFICATION_LARGE_ICON);
            int largeIconResId = hubUtil.getLargeIcon(bundle, largeIcon, res, packageName);
            Bitmap largeIconBitmap = BitmapFactory.decodeResource(res, largeIconResId);
            if (largeIconResId != 0 && (largeIcon != null || Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {
                notificationBuilder.setLargeIcon(largeIconBitmap);
            }

            String bigText = bundle.getString(KEY_REMOTE_NOTIFICATION_BIG_TEXT);
            if (bigText == null) {
                bigText = bundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE);
            }

            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));

            // Create notification intent
            Intent intent = hubUtil.createNotificationIntent(context, bundle, intentClass);

            if (!bundle.containsKey(KEY_REMOTE_NOTIFICATION_PLAY_SOUND) || bundle.getBoolean(KEY_REMOTE_NOTIFICATION_PLAY_SOUND)) {
                Uri soundUri = hubUtil.getSoundUri(context, bundle);
                notificationBuilder.setSound(soundUri);
            }

            if (bundle.containsKey(KEY_REMOTE_NOTIFICATION_ONGOING)) {
                notificationBuilder.setOngoing(bundle.getBoolean(KEY_REMOTE_NOTIFICATION_ONGOING));
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                notificationBuilder.setCategory(NotificationCompat.CATEGORY_CALL);

                String color = bundle.getString(KEY_REMOTE_NOTIFICATION_COLOR);
                if (color != null) {
                    notificationBuilder.setColor(Color.parseColor(color));
                }
            }

            int notificationID = notificationIdString.hashCode();

            PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationID, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            notificationBuilder.setContentIntent(pendingIntent);

            if (!bundle.containsKey(KEY_REMOTE_NOTIFICATION_VIBRATE) || bundle.getBoolean(KEY_REMOTE_NOTIFICATION_VIBRATE)) {
                long vibration = bundle.containsKey(KEY_REMOTE_NOTIFICATION_VIBRATION) ?
                        (long) bundle.getDouble(KEY_REMOTE_NOTIFICATION_VIBRATION) : DEFAULT_VIBRATION;
                if (vibration == 0)
                    vibration = DEFAULT_VIBRATION;
                notificationBuilder.setVibrate(new long[]{0, vibration});
            }

            // Process notification's actions
            hubUtil.processNotificationActions(context, bundle, notificationBuilder, notificationID);

            Notification notification = notificationBuilder.build();
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                    Context.NOTIFICATION_SERVICE);
            if (bundle.containsKey(KEY_REMOTE_NOTIFICATION_TAG)) {
                String tag = bundle.getString(KEY_REMOTE_NOTIFICATION_TAG);
                notificationManager.notify(tag, notificationID, notification);
            } else {
                notificationManager.notify(notificationID, notification);
            }
        } catch (Exception e) {
            Log.e(TAG, ERROR_SEND_PUSH_NOTIFICATION, e);
        }
    }
}
