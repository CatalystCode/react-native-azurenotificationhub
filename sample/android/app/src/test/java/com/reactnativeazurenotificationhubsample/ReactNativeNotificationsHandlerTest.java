package com.reactnativeazurenotificationhubsample;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.azure.reactnative.notificationhub.NotificationHubUtil;
import com.azure.reactnative.notificationhub.ReactNativeNotificationsHandler;
import com.facebook.react.bridge.ReactApplicationContext;

/**
 * Unit tests for ReactNativeNotificationHubModule.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
        LocalBroadcastManager.class,
        NotificationHubUtil.class,
        ReactNativeNotificationsHandler.class,
        Log.class
})
public class ReactNativeNotificationsHandlerTest {
    private final static String CHANNEL_ID = "Channel ID";

    @Mock
    ReactApplicationContext mReactApplicationContext;

    @Mock
    NotificationHubUtil mNotificationHubUtil;

    @Mock
    LocalBroadcastManager mLocalBroadcastManager;

    Class mIntentClass;

    @Before
    public void setUp() throws Exception {
        // Prepare mock objects
        PowerMockito.mockStatic(LocalBroadcastManager.class);
        when(LocalBroadcastManager.getInstance(mReactApplicationContext)).thenReturn(mLocalBroadcastManager);
        PowerMockito.mockStatic(NotificationHubUtil.class);
        when(NotificationHubUtil.getInstance()).thenReturn(mNotificationHubUtil);
        PowerMockito.mockStatic(Log.class);

        // Reset mocks
        reset(mLocalBroadcastManager);
        reset(mNotificationHubUtil);

        mIntentClass = Class.forName("com.reactnativeazurenotificationhubsample.MainActivity");
    }

    @Test
    public void testSendBroadcast() throws Exception {
        final int delay = 1000;

        Bundle bundle = PowerMockito.mock(Bundle.class);
        JSONObject json = PowerMockito.mock(JSONObject.class);
        when(mNotificationHubUtil.convertBundleToJSON(bundle)).thenReturn(json);

        Intent intent = PowerMockito.mock(Intent.class);
        when(mNotificationHubUtil.createBroadcastIntent(TAG, json)).thenReturn(intent);

        ArgumentCaptor<Runnable> workerTask = ArgumentCaptor.forClass(Runnable.class);
        Mockito.doNothing().when(mNotificationHubUtil).runInWorkerThread(workerTask.capture());

        sendBroadcast(mReactApplicationContext, bundle, delay);
        workerTask.getValue().run();

        verify(mNotificationHubUtil, times(1)).runInWorkerThread(any(Runnable.class));
        verify(mNotificationHubUtil, times(1)).convertBundleToJSON(bundle);
        verify(mNotificationHubUtil, times(1)).createBroadcastIntent(TAG, json);
        verify(mLocalBroadcastManager, times(1)).sendBroadcast(intent);
    }

    @Test
    public void testSendNotificationNoActivityClass() {
        Class intentClass = null;
        when(mNotificationHubUtil.getMainActivityClass(mReactApplicationContext)).thenReturn(intentClass);
        Bundle bundle = PowerMockito.mock(Bundle.class);

        sendNotification(mReactApplicationContext, bundle, CHANNEL_ID);

        PowerMockito.verifyStatic(Log.class);
        Log.e(TAG, ERROR_NO_ACTIVITY_CLASS);
    }

    @Test
    public void testSendNotificationNoMessage() {
        when(mNotificationHubUtil.getMainActivityClass(mReactApplicationContext)).thenReturn(mIntentClass);
        Bundle bundle = PowerMockito.mock(Bundle.class);

        sendNotification(mReactApplicationContext, bundle, CHANNEL_ID);

        PowerMockito.verifyStatic(Log.class);
        Log.e(TAG, ERROR_NO_MESSAGE);
    }

    @Test
    public void testSendNotificationNoNotifID() {
        when(mNotificationHubUtil.getMainActivityClass(mReactApplicationContext)).thenReturn(mIntentClass);
        Bundle bundle = PowerMockito.mock(Bundle.class);
        when(bundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE)).thenReturn("message");

        sendNotification(mReactApplicationContext, bundle, CHANNEL_ID);

        PowerMockito.verifyStatic(Log.class);
        Log.e(TAG, ERROR_NO_NOTIF_ID);
    }
}