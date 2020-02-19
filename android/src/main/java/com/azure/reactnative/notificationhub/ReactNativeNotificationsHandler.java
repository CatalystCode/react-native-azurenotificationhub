package com.azure.reactnative.notificationhub;

import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

public class ReactNativeNotificationsHandler {
    public static final String TAG = "ReactNativeNotification";

    private static final long DEFAULT_VIBRATION = 300L;

    private static Context appContext;
    private static boolean channelCreated;
    private static NotificationChannel notificationChannel;

    public void sendBroadcast(final Bundle bundle, final long delay) {
        (new Thread() {
            public void run() {
                try {
                    Thread.currentThread().sleep(delay);
                    JSONObject json = new JSONObject();
                    Set<String> keys = bundle.keySet();
                    for (String key : keys) {
                        try {
                            json.put(key, bundle.get(key));
                        } catch (JSONException e) {
                        }
                    }

                    Intent event = new Intent(TAG);
                    event.putExtra("event", ReactNativeNotificationHubModule.DEVICE_NOTIF_EVENT);
                    event.putExtra("data", json.toString());
                    LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(appContext);
                    localBroadcastManager.sendBroadcast(event);
                } catch (Exception e) {
                }
            }
        }).start();
    }

    public void sendNotification(Bundle bundle) {
        try {
            Class intentClass = getMainActivityClass();
            if (intentClass == null) {
                Log.e(TAG, "No activity class found for the notification");
                return;
            }

            if (bundle.getString("message") == null) {
                Log.e(TAG, "No message specified for the notification");
                return;
            }

            String notificationIdString = bundle.getString("google.message_id");
            if (notificationIdString == null) {
                Log.e(TAG, "No notification ID specified for the notification");
                return;
            }

            Resources res = appContext.getResources();
            String packageName = appContext.getPackageName();

            String title = bundle.getString("title");
            if (title == null) {
                ApplicationInfo appInfo = appContext.getApplicationInfo();
                title = appContext.getPackageManager().getApplicationLabel(appInfo).toString();
            }

            int priority = NotificationCompat.PRIORITY_DEFAULT;
            final String priorityString = bundle.getString("google.original_priority");
            if (priorityString != null) {
                switch (priorityString.toLowerCase()) {
                    case "max":
                        priority = NotificationCompat.PRIORITY_MAX;
                        break;
                    case "high":
                        priority = NotificationCompat.PRIORITY_HIGH;
                        break;
                    case "low":
                        priority = NotificationCompat.PRIORITY_LOW;
                        break;
                    case "min":
                        priority = NotificationCompat.PRIORITY_MIN;
                        break;
                    case "normal":
                        priority = NotificationCompat.PRIORITY_DEFAULT;
                        break;
                }
            }

            NotificationCompat.Builder notification = new NotificationCompat.Builder(appContext, notificationChannel.getId())
                    .setContentTitle(title)
                    .setTicker(bundle.getString("ticker"))
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setPriority(priority)
                    .setAutoCancel(bundle.getBoolean("autoCancel", true));

            String group = bundle.getString("group");
            if (group != null) {
                notification.setGroup(group);
            }

            notification.setContentText(bundle.getString("message"));

            String largeIcon = bundle.getString("largeIcon");

            String subText = bundle.getString("subText");

            if (subText != null) {
                notification.setSubText(subText);
            }

            String numberString = bundle.getString("number");
            if (numberString != null) {
                notification.setNumber(Integer.parseInt(numberString));
            }

            int smallIconResId;
            int largeIconResId;

            String smallIcon = bundle.getString("smallIcon");

            if (smallIcon != null) {
                smallIconResId = res.getIdentifier(smallIcon, "mipmap", packageName);
            } else {
                smallIconResId = res.getIdentifier("ic_notification", "mipmap", packageName);
            }

            if (smallIconResId == 0) {
                smallIconResId = res.getIdentifier("ic_launcher", "mipmap", packageName);

                if (smallIconResId == 0) {
                    smallIconResId = android.R.drawable.ic_dialog_info;
                }
            }

            if (largeIcon != null) {
                largeIconResId = res.getIdentifier(largeIcon, "mipmap", packageName);
            } else {
                largeIconResId = res.getIdentifier("ic_launcher", "mipmap", packageName);
            }

            Bitmap largeIconBitmap = BitmapFactory.decodeResource(res, largeIconResId);

            if (largeIconResId != 0 && (largeIcon != null || Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {
                notification.setLargeIcon(largeIconBitmap);
            }

            notification.setSmallIcon(smallIconResId);
            String bigText = bundle.getString("bigText");

            if (bigText == null) {
                bigText = bundle.getString("message");
            }

            notification.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));

            Intent intent = new Intent(appContext, intentClass);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            bundle.putBoolean("userInteraction", true);
            intent.putExtra("notification", bundle);

            if (!bundle.containsKey("playSound") || bundle.getBoolean("playSound")) {
                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                String soundName = bundle.getString("soundName");
                if (soundName != null) {
                    if (!"default".equalsIgnoreCase(soundName)) {

                        // sound name can be full filename, or just the resource name.
                        // So the strings 'my_sound.mp3' AND 'my_sound' are accepted
                        // The reason is to make the iOS and android javascript interfaces compatible

                        int resId;
                        if (appContext.getResources().getIdentifier(soundName, "raw", appContext.getPackageName()) != 0) {
                            resId = appContext.getResources().getIdentifier(soundName, "raw", appContext.getPackageName());
                        } else {
                            soundName = soundName.substring(0, soundName.lastIndexOf('.'));
                            resId = appContext.getResources().getIdentifier(soundName, "raw", appContext.getPackageName());
                        }

                        soundUri = Uri.parse("android.resource://" + appContext.getPackageName() + "/" + resId);
                    }
                }
                notification.setSound(soundUri);
            }

            if (bundle.containsKey("ongoing") || bundle.getBoolean("ongoing")) {
                notification.setOngoing(bundle.getBoolean("ongoing"));
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                notification.setCategory(NotificationCompat.CATEGORY_CALL);

                String color = bundle.getString("color");
                if (color != null) {
                    notification.setColor(Color.parseColor(color));
                }
            }

            int notificationID = notificationIdString.hashCode();

            PendingIntent pendingIntent = PendingIntent.getActivity(appContext, notificationID, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            notification.setContentIntent(pendingIntent);

            if (!bundle.containsKey("vibrate") || bundle.getBoolean("vibrate")) {
                long vibration = bundle.containsKey("vibration") ? (long) bundle.getDouble("vibration") : DEFAULT_VIBRATION;
                if (vibration == 0)
                    vibration = DEFAULT_VIBRATION;
                notification.setVibrate(new long[]{0, vibration});
            }

            JSONArray actionsArray = null;
            try {
                actionsArray = bundle.getString("actions") != null ? new JSONArray(bundle.getString("actions")) : null;
            } catch (JSONException e) {
                Log.e(TAG, "Exception while converting actions to JSON object.", e);
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
                        Log.e(TAG, "Exception while getting action from actionsArray.", e);
                        continue;
                    }

                    Intent actionIntent = new Intent();
                    actionIntent.setAction(appContext.getPackageName() + "." + action);
                    // Add "action" for later identifying which button gets pressed.
                    bundle.putString("action", action);
                    actionIntent.putExtra("notification", bundle);
                    PendingIntent pendingActionIntent = PendingIntent.getBroadcast(appContext, notificationID, actionIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                    notification.addAction(icon, action, pendingActionIntent);
                }
            }

            Notification info = notification.build();
            NotificationManager notificationManager = appContext.getSystemService(NotificationManager.class);
            if (bundle.containsKey("tag")) {
                String tag = bundle.getString("tag");
                notificationManager.notify(tag, notificationID, info);
            } else {
                notificationManager.notify(notificationID, info);
            }
        } catch (Exception e) {
            Log.e(TAG, "failed to send push notification", e);
        }
    }

    public void createChannelAndHandleNotifications(Context context, NotificationChannel channel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !channelCreated) {
            appContext = context;
            notificationChannel = channel;
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            channelCreated = true;
        }
    }

    private Class getMainActivityClass() {
        String packageName = appContext.getPackageName();
        Intent launchIntent = appContext.getPackageManager().getLaunchIntentForPackage(packageName);
        String className = launchIntent.getComponent().getClassName();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
