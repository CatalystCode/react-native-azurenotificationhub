package com.reactnativeazurenotificationhubsample;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.junit.Before;
import org.junit.Test;

import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.*;
import static com.azure.reactnative.notificationhub.ReactNativeConstants.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.azure.reactnative.notificationhub.ReactNativeNotificationHubUtil;
import com.azure.reactnative.notificationhub.ReactNativeUtil;
import com.facebook.react.bridge.ReactApplicationContext;

/**
 * Unit tests for ReactNativeNotificationHubModule.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
        LocalBroadcastManager.class,
        ReactNativeNotificationHubUtil.class,
        ReactNativeUtil.class,
        BitmapFactory.class,
        Build.VERSION.class,
        Color.class,
        PendingIntent.class,
        Log.class
})
public class ReactNativeNotificationsHandlerTest {
    private final static String CHANNEL_ID = "Channel ID";
    private final static String NOTIFICATION_ID = "Notification ID";
    private final static String NOTIFICATION_TITLE = "Notification Title";
    private final static String NOTIFICATION_MESSAGE = "Notification Message";
    private final static String NOTIFICATION_PRIORITY = "normal";

    @Mock
    ReactApplicationContext mReactApplicationContext;

    @Mock
    ReactNativeNotificationHubUtil mNotificationHubUtil;

    @Mock
    LocalBroadcastManager mLocalBroadcastManager;

    @Mock
    NotificationCompat.Builder mNotificationBuilder;

    @Mock
    Notification mNotification;

    @Mock
    Bundle mBundle;

    private Class mIntentClass;
    private ArgumentCaptor<Runnable> mWorkerTask;

    @Before
    public void setUp() throws Exception {
        // Reset mocks
        reset(mLocalBroadcastManager);
        reset(mNotificationHubUtil);
        reset(mBundle);

        // Prepare mock objects
        PowerMockito.mockStatic(LocalBroadcastManager.class);
        when(LocalBroadcastManager.getInstance(mReactApplicationContext)).thenReturn(mLocalBroadcastManager);
        PowerMockito.mockStatic(ReactNativeNotificationHubUtil.class);
        when(ReactNativeNotificationHubUtil.getInstance()).thenReturn(mNotificationHubUtil);
        PowerMockito.mockStatic(ReactNativeUtil.class);
        PowerMockito.mockStatic(BitmapFactory.class);
        PowerMockito.mockStatic(Color.class);
        PowerMockito.mockStatic(PendingIntent.class);
        PowerMockito.mockStatic(Log.class);

        mIntentClass = Class.forName("com.reactnativeazurenotificationhubsample.MainActivity");
        when(ReactNativeUtil.getMainActivityClass(mReactApplicationContext)).thenReturn(mIntentClass);
        mWorkerTask = ArgumentCaptor.forClass(Runnable.class);
        PowerMockito.doNothing().when(
                ReactNativeUtil.class, "runInWorkerThread", mWorkerTask.capture());
        mNotificationBuilder = PowerMockito.mock(NotificationCompat.Builder.class);
        when(ReactNativeUtil.initNotificationCompatBuilder(
                any(), any(), any(), any(), anyInt(), anyInt(), anyBoolean())).thenReturn(mNotificationBuilder);
        mNotification = PowerMockito.mock(Notification.class);
        when(mNotificationBuilder.build()).thenReturn(mNotification);
    }

    @Test
    public void testSendBroadcastIntent() throws Exception {
        final int delay = 1000;

        Intent intent = PowerMockito.mock(Intent.class);

        sendBroadcast(mReactApplicationContext, intent, delay);
        mWorkerTask.getValue().run();

        PowerMockito.verifyStatic(ReactNativeUtil.class);
        ReactNativeUtil.runInWorkerThread(any(Runnable.class));
        verify(mLocalBroadcastManager, times(1)).sendBroadcast(intent);
    }

    @Test
    public void testSendBroadcastBundle() throws Exception {
        final int delay = 1000;

        Intent intent = PowerMockito.mock(Intent.class);
        when(ReactNativeUtil.createBroadcastIntent(TAG, mBundle)).thenReturn(intent);

        sendBroadcast(mReactApplicationContext, mBundle, delay);
        mWorkerTask.getValue().run();

        PowerMockito.verifyStatic(ReactNativeUtil.class);
        ReactNativeUtil.runInWorkerThread(any(Runnable.class));
        PowerMockito.verifyStatic(ReactNativeUtil.class);
        ReactNativeUtil.createBroadcastIntent(TAG, mBundle);
        verify(mLocalBroadcastManager, times(1)).sendBroadcast(intent);
    }

    @Test
    public void testSendNotificationNoActivityClass() {
        Class intentClass = null;
        when(ReactNativeUtil.getMainActivityClass(mReactApplicationContext)).thenReturn(intentClass);
        Bundle bundle = PowerMockito.mock(Bundle.class);

        sendNotification(mReactApplicationContext, bundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        PowerMockito.verifyStatic(Log.class);
        Log.e(TAG, ERROR_NO_ACTIVITY_CLASS);
    }

    @Test
    public void testSendNotificationNoMessage() {
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_BODY)).thenReturn(null);
        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();
        PowerMockito.verifyStatic(Log.class, times(0));
        Log.e(TAG, ERROR_NO_MESSAGE);

        reset(mBundle);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(null);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_BODY)).thenReturn(NOTIFICATION_MESSAGE);
        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();
        PowerMockito.verifyStatic(Log.class, times(0));
        Log.e(TAG, ERROR_NO_MESSAGE);

        reset(mBundle);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(null);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_BODY)).thenReturn(null);
        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();
        PowerMockito.verifyStatic(Log.class);
        Log.e(TAG, ERROR_NO_MESSAGE);
    }

    @Test
    public void testSendNotificationNoTitle() {
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);

        ApplicationInfo appInfo = PowerMockito.mock(ApplicationInfo.class);
        PackageManager packageManager = PowerMockito.mock(PackageManager.class);
        CharSequence applicationLabel = PowerMockito.mock(CharSequence.class);
        when(mReactApplicationContext.getApplicationInfo()).thenReturn(appInfo);
        when(mReactApplicationContext.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getApplicationLabel(appInfo)).thenReturn(applicationLabel);
        when(applicationLabel.toString()).thenReturn(NOTIFICATION_TITLE);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        PowerMockito.verifyStatic(ReactNativeUtil.class);
        ReactNativeUtil.initNotificationCompatBuilder(
                any(), any(), eq(NOTIFICATION_TITLE), any(), anyInt(), anyInt(), anyBoolean());
    }

    @Test
    public void testSendNotificationHasTitle() {
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        PowerMockito.verifyStatic(ReactNativeUtil.class);
        ReactNativeUtil.initNotificationCompatBuilder(
                any(), any(), eq(NOTIFICATION_TITLE), any(), anyInt(), anyInt(), anyBoolean());
        verify(mNotificationBuilder, times(0)).setGroup(any());
        verify(mNotificationBuilder, times(0)).setSubText(any());
        verify(mNotificationBuilder, times(0)).setNumber(anyInt());
        verify(mNotificationBuilder, times(0)).setOngoing(anyBoolean());
        verify(mNotificationBuilder, times(0)).setColor(anyInt());
    }

    @Test
    public void testSendNotificationNoPriority() {
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        PowerMockito.verifyStatic(ReactNativeUtil.class);
        ReactNativeUtil.getNotificationCompatPriority(null);
    }

    @Test
    public void testSendNotificationHasPriority() {
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_PRIORITY)).thenReturn(NOTIFICATION_PRIORITY);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        PowerMockito.verifyStatic(ReactNativeUtil.class);
        ReactNativeUtil.getNotificationCompatPriority(NOTIFICATION_PRIORITY);
    }

    @Test
    public void testSendNotificationHasGroup() {
        final String group = "Notification Group";

        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_GROUP)).thenReturn(group);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        verify(mNotificationBuilder, times(1)).setGroup(group);
    }

    @Test
    public void testSendNotificationSetContentText() {
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        verify(mNotificationBuilder, times(1)).setContentText(NOTIFICATION_MESSAGE);
    }

    @Test
    public void testSendNotificationHasSubText() {
        final String subText = "Sub Text";

        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_SUB_TEXT)).thenReturn(subText);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        verify(mNotificationBuilder, times(1)).setSubText(subText);
    }

    @Test
    public void testSendNotificationHasNumber() {
        final String strNumber = "1";

        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_NUMBER)).thenReturn(strNumber);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        verify(mNotificationBuilder, times(1)).setNumber(Integer.parseInt(strNumber));
    }

    @Test
    public void testSendNotificationHasSmallIcon() {
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        PowerMockito.verifyStatic(ReactNativeUtil.class);
        ReactNativeUtil.getSmallIcon(any(), any(), any());
        verify(mNotificationBuilder, times(1)).setSmallIcon(anyInt());
    }

    @Test
    public void testSendNotificationNoLargeIcon() {
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        PowerMockito.verifyStatic(ReactNativeUtil.class);
        ReactNativeUtil.getLargeIcon(any(), eq(null), any(), any());
        verify(mNotificationBuilder, times(0)).setLargeIcon(any());
    }

    @Test
    public void testSendNotificationHasLargeIcon() {
        final String largeIcon = "Large Icon";
        final int largeIconResID = 1;

        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_LARGE_ICON)).thenReturn(largeIcon);
        when(ReactNativeUtil.getLargeIcon(any(), any(), any(), any())).thenReturn(largeIconResID);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        PowerMockito.verifyStatic(ReactNativeUtil.class);
        ReactNativeUtil.getLargeIcon(any(), eq(largeIcon), any(), any());
        verify(mNotificationBuilder, times(1)).setLargeIcon(any());
    }

    @Test
    public void testSendNotificationHasAvatarUrl() {
        final String url = "http://avatar.com/1.png";

        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_AVATAR_URL)).thenReturn(url);
        Bitmap bitmap = PowerMockito.mock(Bitmap.class);
        when(ReactNativeUtil.fetchImage(url)).thenReturn(bitmap);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        verify(mNotificationBuilder, times(1)).setLargeIcon(bitmap);
    }

    @Test
    public void testSendNotificationHasAvatarUrlFetchFailed() {
        final String url = "http://avatar.com/1.png";

        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_AVATAR_URL)).thenReturn(url);
        when(ReactNativeUtil.fetchImage(url)).thenReturn(null);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        verify(mNotificationBuilder, times(0)).setLargeIcon(any());
    }

    @Test
    public void testSendNotificationHasBigText() {
        final String bigText = "Big Text";

        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_BIG_TEXT)).thenReturn(bigText);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        verify(mBundle, times(1)).getString(KEY_REMOTE_NOTIFICATION_BIG_TEXT);
        PowerMockito.verifyStatic(ReactNativeUtil.class);
        ReactNativeUtil.getBigTextStyle(bigText);
    }

    @Test
    public void testSendNotificationNoBigTextMessage() {
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn("message");
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_BIG_TEXT)).thenReturn(null);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        verify(mBundle, times(1)).getString(KEY_REMOTE_NOTIFICATION_BIG_TEXT);
        PowerMockito.verifyStatic(ReactNativeUtil.class);
        ReactNativeUtil.getBigTextStyle("message");
    }

    @Test
    public void testSendNotificationNoBigTextBody() {
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_BODY)).thenReturn("body");
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_BIG_TEXT)).thenReturn(null);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        verify(mBundle, times(1)).getString(KEY_REMOTE_NOTIFICATION_BIG_TEXT);
        PowerMockito.verifyStatic(ReactNativeUtil.class);
        ReactNativeUtil.getBigTextStyle("body");
    }

    @Test
    public void testSendNotificationCreateNotificationIntent() {
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        PowerMockito.verifyStatic(ReactNativeUtil.class);
        ReactNativeUtil.createNotificationIntent(any(), any(), any());
    }

    @Test
    public void testSendNotificationHasPlaySound() {
        final String playSound = "Play Sound";

        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);
        when(mBundle.getBoolean(KEY_REMOTE_NOTIFICATION_PLAY_SOUND)).thenReturn(true);
        Uri soundUri = PowerMockito.mock(Uri.class);
        when(ReactNativeUtil.getSoundUri(any(), any())).thenReturn(soundUri);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        PowerMockito.verifyStatic(ReactNativeUtil.class);
        ReactNativeUtil.getSoundUri(any(), any());
        verify(mNotificationBuilder, times(1)).setSound(soundUri);
    }

    @Test
    public void testSendNotificationDisableSound() {
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);
        when(mBundle.containsKey(KEY_REMOTE_NOTIFICATION_PLAY_SOUND)).thenReturn(true);
        when(mBundle.getBoolean(KEY_REMOTE_NOTIFICATION_PLAY_SOUND)).thenReturn(false);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        PowerMockito.verifyStatic(ReactNativeUtil.class, times(0));
        ReactNativeUtil.getSoundUri(any(), any());
    }

    @Test
    public void testSendNotificationHasOngoing() {
        final boolean ongoing = true;

        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);
        when(mBundle.containsKey(KEY_REMOTE_NOTIFICATION_ONGOING)).thenReturn(true);
        when(mBundle.getBoolean(KEY_REMOTE_NOTIFICATION_ONGOING)).thenReturn(ongoing);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        verify(mNotificationBuilder, times(1)).setOngoing(ongoing);
    }

    @Test
    public void testSendNotificationHasColor() throws Exception {
        final int sdkVersion = Build.VERSION_CODES.LOLLIPOP;
        final String color = "#FF00FF";

        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_COLOR)).thenReturn(color);

        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", sdkVersion);
        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        verify(mNotificationBuilder, times(1)).setCategory(NotificationCompat.CATEGORY_CALL);
        verify(mNotificationBuilder, times(1)).setColor(Color.parseColor(color));
    }

    @Test
    public void testSendNotificationSetContentIntent() {
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_ID)).thenReturn(NOTIFICATION_ID);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        verify(mNotificationBuilder, times(1)).setContentIntent(any());
    }

    @Test
    public void testSendNotificationDisableVibration() {
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);
        when(mBundle.containsKey(KEY_REMOTE_NOTIFICATION_VIBRATE)).thenReturn(true);
        when(mBundle.getBoolean(KEY_REMOTE_NOTIFICATION_VIBRATE)).thenReturn(false);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        verify(mNotificationBuilder, times(0)).setVibrate(any());
    }

    @Test
    public void testSendNotificationHasVibration() {
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_ID)).thenReturn(NOTIFICATION_ID);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);
        when(mBundle.containsKey(KEY_REMOTE_NOTIFICATION_VIBRATE)).thenReturn(false);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        verify(mNotificationBuilder, times(1)).setVibrate(any());
    }

    @Test
    public void testSendNotificationProcessActions() {
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_ID)).thenReturn(NOTIFICATION_ID);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        PowerMockito.verifyStatic(ReactNativeUtil.class);
        ReactNativeUtil.processNotificationActions(any(), any(), any(), anyInt());
    }

    @Test
    public void testSendNotificationNotifyNoTag() {
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_ID)).thenReturn(NOTIFICATION_ID);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);
        NotificationManager notificationManager = PowerMockito.mock(NotificationManager.class);
        when(mReactApplicationContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(
                notificationManager);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        PowerMockito.verifyStatic(ReactNativeUtil.class);
        ReactNativeUtil.processNotificationActions(any(), any(), any(), anyInt());
        verify(notificationManager, times(1)).notify(anyInt(), eq(mNotification));
    }

    @Test
    public void testSendNotificationNotifyHasTag() {
        final String tags = "[ Tag0, Tag1, Tag2 ]";

        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_ID)).thenReturn(NOTIFICATION_ID);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn(NOTIFICATION_MESSAGE);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TITLE)).thenReturn(NOTIFICATION_TITLE);
        when(mBundle.containsKey(KEY_REMOTE_NOTIFICATION_TAG)).thenReturn(true);
        when(mBundle.getString(KEY_REMOTE_NOTIFICATION_TAG)).thenReturn(tags);
        NotificationManager notificationManager = PowerMockito.mock(NotificationManager.class);
        when(mReactApplicationContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(
                notificationManager);

        sendNotification(mReactApplicationContext, mBundle, CHANNEL_ID);
        mWorkerTask.getValue().run();

        PowerMockito.verifyStatic(ReactNativeUtil.class);
        ReactNativeUtil.processNotificationActions(any(), any(), any(), anyInt());
        verify(notificationManager, times(1)).notify(eq(tags), anyInt(), eq(mNotification));
    }
}
