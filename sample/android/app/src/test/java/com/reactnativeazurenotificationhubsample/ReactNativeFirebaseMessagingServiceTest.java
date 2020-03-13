package com.reactnativeazurenotificationhubsample;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.azure.reactnative.notificationhub.NotificationChannelBuilder;
import com.azure.reactnative.notificationhub.NotificationHubUtil;
import com.azure.reactnative.notificationhub.ReactNativeFirebaseMessagingService;
import com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler;
import com.azure.reactnative.notificationhub.ReactNativeRegistrationIntentService;
import com.facebook.react.bridge.ReactApplicationContext;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static com.azure.reactnative.notificationhub.Constants.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.support.membermodification.MemberMatcher.methodsDeclaredIn;

/**
 * Unit tests for ReactNativeNotificationHubModule.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
        NotificationHubUtil.class,
        ReactNativeNotificationsHandler.class,
        ReactNativeRegistrationIntentService.class,
        NotificationChannelBuilder.Factory.class,
        NotificationHubUtil.IntentFactory.class,
        Build.VERSION.class,
        FirebaseMessagingService.class,
        RemoteMessage.class,
        Log.class
})
public class ReactNativeFirebaseMessagingServiceTest {
    @Mock
    ReactApplicationContext mReactApplicationContext;

    @Mock
    NotificationHubUtil mHubUtil;

    @Mock
    NotificationManager mNotificationManager;

    ReactNativeFirebaseMessagingService mMessagingService;

    @Before
    public void setUp() {
        // Reset mocks
        reset(mHubUtil);
        reset(mReactApplicationContext);

        // Prepare mock objects
        PowerMockito.mockStatic(NotificationHubUtil.class);
        when(NotificationHubUtil.getInstance()).thenReturn(mHubUtil);
        PowerMockito.mockStatic(ReactNativeNotificationsHandler.class);
        PowerMockito.mockStatic(ReactNativeRegistrationIntentService.class);
        PowerMockito.mockStatic(NotificationChannelBuilder.Factory.class);
        PowerMockito.mockStatic(NotificationHubUtil.IntentFactory.class);
        PowerMockito.suppress(methodsDeclaredIn(FirebaseMessagingService.class));
        PowerMockito.mockStatic(Log.class);
        when(mReactApplicationContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(
                mNotificationManager);

        // Reset channel
        ReactNativeFirebaseMessagingService.deleteNotificationChannel(mReactApplicationContext);
        reset(mNotificationManager);
        when(mReactApplicationContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(
                mNotificationManager);

        mMessagingService = new ReactNativeFirebaseMessagingService();

    }

    @Test
    public void testCreateNotificationChannelBelowVersionO() {
        final int sdkVersion = Build.VERSION_CODES.LOLLIPOP;

        NotificationChannelBuilder builder = PowerMockito.mock(NotificationChannelBuilder.class);
        when(NotificationChannelBuilder.Factory.create()).thenReturn(builder);
        when(mHubUtil.hasChannelImportance(mReactApplicationContext)).thenReturn(false);
        when(mHubUtil.hasChannelShowBadge(mReactApplicationContext)).thenReturn(false);
        when(mHubUtil.hasChannelEnableLights(mReactApplicationContext)).thenReturn(false);
        when(mHubUtil.hasChannelEnableVibration(mReactApplicationContext)).thenReturn(false);
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", sdkVersion);

        ReactNativeFirebaseMessagingService.createNotificationChannel(mReactApplicationContext);

        verify(builder, times(0)).setImportance(anyInt());
        verify(builder, times(0)).setShowBadge(anyBoolean());
        verify(builder, times(0)).enableLights(anyBoolean());
        verify(builder, times(0)).enableVibration(anyBoolean());
        verify(builder, times(0)).build();
        verify(mNotificationManager, times(0)).createNotificationChannel(
                any(NotificationChannel.class));
    }

    @Test
    public void testCreateNotificationChannel() {
        final int channelImportance = 1;
        final boolean channelShowBadge = true;
        final boolean channelEnableLights = true;
        final boolean channelEnableVibration = true;
        final int sdkVersion = Build.VERSION_CODES.O;

        NotificationChannelBuilder builder = PowerMockito.mock(NotificationChannelBuilder.class);
        when(NotificationChannelBuilder.Factory.create()).thenReturn(builder);
        NotificationChannel channel = PowerMockito.mock(NotificationChannel.class);
        when(channel.getId()).thenReturn(NOTIFICATION_CHANNEL_ID);
        when(builder.build()).thenReturn(channel);
        when(mHubUtil.hasChannelImportance(mReactApplicationContext)).thenReturn(true);
        when(mHubUtil.getChannelImportance(mReactApplicationContext)).thenReturn(channelImportance);
        when(mHubUtil.hasChannelShowBadge(mReactApplicationContext)).thenReturn(true);
        when(mHubUtil.getChannelShowBadge(mReactApplicationContext)).thenReturn(channelShowBadge);
        when(mHubUtil.hasChannelEnableLights(mReactApplicationContext)).thenReturn(true);
        when(mHubUtil.getChannelEnableLights(mReactApplicationContext)).thenReturn(channelEnableLights);
        when(mHubUtil.hasChannelEnableVibration(mReactApplicationContext)).thenReturn(true);
        when(mHubUtil.getChannelEnableVibration(mReactApplicationContext)).thenReturn(channelEnableVibration);
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", sdkVersion);

        ReactNativeFirebaseMessagingService.createNotificationChannel(mReactApplicationContext);

        verify(mHubUtil, times(1)).hasChannelImportance(mReactApplicationContext);
        verify(builder, times(1)).setImportance(channelImportance);
        verify(mHubUtil, times(1)).hasChannelShowBadge(mReactApplicationContext);
        verify(builder, times(1)).setShowBadge(channelShowBadge);
        verify(mHubUtil, times(1)).hasChannelEnableLights(mReactApplicationContext);
        verify(builder, times(1)).enableLights(channelEnableLights);
        verify(mHubUtil, times(1)).hasChannelEnableVibration(mReactApplicationContext);
        verify(builder, times(1)).enableVibration(channelEnableVibration);
        verify(builder, times(1)).build();
        verify(mNotificationManager, times(1)).createNotificationChannel(
                any(NotificationChannel.class));
    }

    @Test
    public void testDeleteNotificationChannel() {
        final int sdkVersion = Build.VERSION_CODES.O;

        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", sdkVersion);

        // Prepare channel
        NotificationChannelBuilder builder = PowerMockito.mock(NotificationChannelBuilder.class);
        when(NotificationChannelBuilder.Factory.create()).thenReturn(builder);
        NotificationChannel channel = PowerMockito.mock(NotificationChannel.class);
        when(channel.getId()).thenReturn(NOTIFICATION_CHANNEL_ID);
        when(builder.build()).thenReturn(channel);
        ReactNativeFirebaseMessagingService.createNotificationChannel(mReactApplicationContext);

        ReactNativeFirebaseMessagingService.deleteNotificationChannel(mReactApplicationContext);

        verify(mNotificationManager, times(1)).deleteNotificationChannel(
                NOTIFICATION_CHANNEL_ID);
    }

    @Test
    public void testOnNewToken() {
        final String token = "Token";

        mMessagingService.onNewToken(token);

        verifyStatic(NotificationHubUtil.IntentFactory.class);
        NotificationHubUtil.IntentFactory.createIntent(
                any(), eq(ReactNativeRegistrationIntentService.class));
    }

    @Test
    public void testOnMessageReceivedForeground() {
        RemoteMessage remoteMessage = PowerMockito.mock(RemoteMessage.class);
        Intent intent = PowerMockito.mock(Intent.class);
        Bundle bundle = PowerMockito.mock(Bundle.class);
        when(remoteMessage.toIntent()).thenReturn(intent);
        when(intent.getExtras()).thenReturn(bundle);
        when(mHubUtil.getAppIsForeground()).thenReturn(true);

        // Prepare channel
        NotificationChannelBuilder builder = PowerMockito.mock(NotificationChannelBuilder.class);
        when(NotificationChannelBuilder.Factory.create()).thenReturn(builder);
        NotificationChannel channel = PowerMockito.mock(NotificationChannel.class);
        when(channel.getId()).thenReturn(NOTIFICATION_CHANNEL_ID);
        when(builder.build()).thenReturn(channel);
        ReactNativeFirebaseMessagingService.createNotificationChannel(mReactApplicationContext);

        mMessagingService.onMessageReceived(remoteMessage);

        verify(bundle, times(1)).putBoolean(
                KEY_REMOTE_NOTIFICATION_FOREGROUND, true);
        verify(bundle, times(1)).putBoolean(
                KEY_REMOTE_NOTIFICATION_USER_INTERACTION, false);
        verify(bundle, times(1)).putBoolean(
                KEY_REMOTE_NOTIFICATION_COLDSTART, false);
        PowerMockito.verifyStatic(ReactNativeNotificationsHandler.class);
        ReactNativeNotificationsHandler.sendBroadcast(any(), eq(bundle), eq((long)0));
    }

    @Test
    public void testOnMessageReceivedBackground() {
        RemoteMessage remoteMessage = PowerMockito.mock(RemoteMessage.class);
        Intent intent = PowerMockito.mock(Intent.class);
        Bundle bundle = PowerMockito.mock(Bundle.class);
        when(remoteMessage.toIntent()).thenReturn(intent);
        when(intent.getExtras()).thenReturn(bundle);
        when(mHubUtil.getAppIsForeground()).thenReturn(false);

        // Prepare channel
        NotificationChannelBuilder builder = PowerMockito.mock(NotificationChannelBuilder.class);
        when(NotificationChannelBuilder.Factory.create()).thenReturn(builder);
        NotificationChannel channel = PowerMockito.mock(NotificationChannel.class);
        when(channel.getId()).thenReturn(NOTIFICATION_CHANNEL_ID);
        when(builder.build()).thenReturn(channel);
        ReactNativeFirebaseMessagingService.createNotificationChannel(mReactApplicationContext);

        mMessagingService.onMessageReceived(remoteMessage);

        PowerMockito.verifyStatic(ReactNativeNotificationsHandler.class);
        ReactNativeNotificationsHandler.sendNotification(any(), eq(bundle), any());
        PowerMockito.verifyStatic(ReactNativeNotificationsHandler.class);
        ReactNativeNotificationsHandler.sendBroadcast(any(), eq(bundle), eq((long)0));
    }
}
